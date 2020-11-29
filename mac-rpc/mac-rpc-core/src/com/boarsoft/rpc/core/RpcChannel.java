package com.boarsoft.rpc.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;
import com.boarsoft.common.util.SocketUtil;
import com.boarsoft.rpc.RpcCallback;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcInvoking;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.serialize.RpcSerializer;

public class RpcChannel {
	private static final Logger log = LoggerFactory.getLogger(RpcChannel.class);

	public static final int STATUS_TEMP = 0;
	public static final int STATUS_NORMAL = 1;
	public static final int STATUS_CLOSING = 2;
	public static final int STATUS_CLOSED = 3;

	protected RpcLink rpcLink;
	protected RpcCore rpcCore;
	protected RpcContext rpcContext;
	protected String remoteAddr;
	protected String localAddr;
	protected AsynchronousSocketChannel asyncSocketChannel;

	protected ExecutorService threadPool;
	protected RpcReader reader = new RpcReader(this);
	protected RpcWriter writer = new RpcWriter(this);

	/** 发送缓冲区队列，用于存入已装填数据，准备写入当前socket通道的缓冲区 */
	protected BlockingQueue<ByteBuffer> outQueue = new LinkedBlockingQueue<ByteBuffer>();
	/** 空缓冲区队列，预先初始化RpcConfig.QUEUE_BUFFERS个ByteBuffer */
	protected BlockingQueue<ByteBuffer> bufQueue = new LinkedBlockingQueue<ByteBuffer>();

	protected AtomicInteger status = new AtomicInteger(STATUS_TEMP);
	protected long createTime;
	protected String key;

	public RpcChannel(RpcCore core, AsynchronousSocketChannel asc) throws IOException {
		this.rpcCore = core;
		this.asyncSocketChannel = asc;
		this.remoteAddr = SocketUtil.getSocketAddressStr(asc.getRemoteAddress());
		this.localAddr = SocketUtil.getSocketAddressStr(asc.getLocalAddress());
		start();
	}

	public RpcChannel(RpcLink link) throws IOException, InterruptedException, ExecutionException, TimeoutException {
		this.rpcLink = link;
		this.rpcCore = link.getRpcCore();
		this.remoteAddr = link.getRemoteAddr();

		log.info("Connecting to {}", this.remoteAddr);
		asyncSocketChannel = AsynchronousSocketChannel.open(rpcCore.getAsyncChannelGroup());
		// 允许重连时重用TIME_WAIT状态的地址和端口，避免过多的TIME_WAIT状态的端口
		asyncSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		// 当连接上无数据传输（IDLE状态）时，让操作系统为我们保持这一连接，而不被关闭
		asyncSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);

		// 连接远程节点
		String[] a = this.remoteAddr.split(":");
		Future<Void> ft = this.asyncSocketChannel.connect(new InetSocketAddress(a[0], Integer.parseInt(a[1])));
		// 控制连接超时
		ft.get(RpcConfig.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
		// 获取当前连接的本地地址（因为端口是随机分配的）
		this.localAddr = SocketUtil.getSocketAddressStr(this.asyncSocketChannel.getLocalAddress());

		start();// 初始化缓冲区队列等变量，并启动相关线程
	}

	protected void start() {
		this.rpcContext = rpcCore.getRpcContext();
		this.threadPool = rpcCore.getThreadPool();
		// 生成当前RpcChannel的标识符
		this.key = new StringBuilder().append(this.localAddr).append("<->")//
				.append(this.remoteAddr).toString();
		log.info("Socket connected {}", this.key);
		// 根据配置预先创建多个ByteBuffer以初始化空缓冲区队列
		for (int i = 0; i < RpcConfig.QUEUE_BUFFERS; i++) {
			this.bufQueue.add(ByteBuffer.allocateDirect(RpcConfig.BUFFER_SIZE));
		}
		// 分别创建并启动数据读取线程和发送线程，实现SocketChannel的读写
		this.reader.start();
		this.writer.start();
		// 记录创建时间，以便在当前逻辑通道超过规定时间未与逻辑连接绑定时移除掉它
		this.createTime = System.currentTimeMillis();
	}

	public String toString() {
		return this.key;
	}

	public boolean isConnected() {
		return (asyncSocketChannel != null) && (asyncSocketChannel.isOpen());
	}

	public void read(final int type, final byte[] data) {
		// 停机会线程池会被关闭，但停机过程中的方法一定要执行完
		if (threadPool.isShutdown()) {
			process(type, data);
		} else {
			// 在当前线程中处理耗时短（数据量小，执行时间短）的请求和响应，有一定的性能优势
			// 但当被执行的方法耗时变长，数据量变大，性能会大幅下降。因此，这里总是由额外的线程来处理
			threadPool.execute(new Runnable() {
				@Override
				public void run() {
					process(type, data);
				}
			});
		}
	}

	public void process(final int type, final byte[] data) {
		RpcCall rc = null;
		try {
			// 反序列化，并区分是请求还是响应，分别处理
			rc = RpcSerializer.deserialize(data);
		} catch (ClassNotFoundException | IOException e) {
			log.error("Error on deserialize RPC data", e);
			this.close("deserialize RPC data failed");
			return;
		}
		if (type == RpcCall.TYPE_REQUEST) {
			onRequest(rc); // 处理RPC请求
		} else if (type == RpcCall.TYPE_RESPONSE) {
			onResponse(rc); // 处理RPC响应
		}
	}

	public void onRequest(final RpcCall co) {
		// 大压力时，心跳时延较高，容易发生超时，导致连接被断开，需要在这里更新
		if (this.rpcLink != null) {
			this.rpcLink.setLastBeat(System.currentTimeMillis());
		}
		// log.debug("Received RPC request {}", co);
		final RpcMethodConfig mc = RpcContext.getMyMethodConfig(co.getMethodId());
		if (mc == null) {
			log.error("Method config of {} is missing", co);
			co.setThrowable(new RejectedExecutionException("The method does not exists"));
			co.setType(RpcCall.TYPE_RESPONSE);
			co.setArguments(RpcCall.EMPTY_ARGS);
			try {
				this.write(co);
			} catch (Throwable e) {
				log.error("Error on write result of {}", co, e);
			}
			return;
		}
		// onRequest总是在工作线程中执行（而不是在IO线程）
		switch (mc.getType()) {
		case RpcMethodConfig.TYPE_SYNC_CALL:
		case RpcMethodConfig.TYPE_ASYNC_CALL:
		case RpcMethodConfig.TYPE_SYNC_BROADCAST:
		case RpcMethodConfig.TYPE_ASYNC_BROADCAST:
			// 非通知类型的调用，同步执行，将结果塞到co中写回
			try {
				rpcContext.invoke(co, mc, this, this.getRemoteAddr());
			} catch (Throwable e) {
				log.error("Error on process request {}", co, e);
				co.setThrowable(e);
			} finally {
				// 总是返回执行结果
				co.setType(RpcCall.TYPE_RESPONSE);
				co.setArguments(RpcCall.EMPTY_ARGS);
				try {
					this.write(co);
				} catch (Exception e) {
					log.error("Error on write response {}", co, e);
				}
			}
			return;
		case RpcMethodConfig.TYPE_SYNC_NOTICE:
		case RpcMethodConfig.TYPE_ASYNC_NOTICE:
		case RpcMethodConfig.TYPE_BROADCAST_NOTICE:
			// 通知类调用总是先向客户端返回回声应答，再执行调用
			RpcCall rc = new RpcCall();
			rc.setProtocol(co.getProtocol());
			rc.setMethodId(co.getMethodId());
			rc.setMethodExeNo(co.getMethodExeNo());
			rc.setType(RpcCall.TYPE_RESPONSE);
			rc.setArguments(RpcCall.EMPTY_ARGS);
			rc.setTraceId(co.getTraceId());
			try {
				// 如果发送失败，也不必再执行了
				this.write(rc);
				rpcContext.invoke(co, mc, this, this.getRemoteAddr());
			} catch (Exception e) {
				log.error("Error on process request {}", co, e);
			}
			return;
		default:
			log.error("Unknown method type {} on {}", mc.getType(), co);
			co.setThrowable(new RejectedExecutionException("Unknown method type"));
			co.setType(RpcCall.TYPE_RESPONSE);
			co.setArguments(RpcCall.EMPTY_ARGS);
			try {
				this.write(co);
			} catch (Exception e) {
				log.error("Error on write response {}", co, e);
			}
			return;
		}
	}

	public void onResponse(final RpcCall co) {
		// log.debug("Received RPC response {}", co);
		// RpcInvoking对象代表一次正在进行的调用，它是在调用发起时被创建并缓存
		RpcInvoking ri = rpcContext.removeInvoking(remoteAddr, co);
		if (ri == null) {
			log.warn("Invoking not exists for {}/{}", remoteAddr, co);
			return;
		}
		// 缓存时保存了RpcCall，这个对象才是原始的RPC请求对象，co仅用于传输
		RpcCall rc = ri.getRpcCall();
		rc.setResult(co.getResult());
		rc.setThrowable(co.getThrowable());
		ri.complete(); // 通知发起调用线程的继续

		// 当远程方法异步执行完成，返回结果时，调起配置的回调
		RpcMethodConfig mc = ri.getMethodConfig();
		String cb = mc.getCallback();
		if (Util.strIsNotEmpty(cb)) {
			if (rpcContext.containsBean(cb)) {
				RpcCallback cf = rpcContext.getBean(cb, RpcCallback.class);
				cf.callback(co.getResult(), co.getArguments());
			}
		}
		return;
	}

	/**
	 * 将RpcCall对象序列化，并拆分为若干数据包，放入发送队列
	 * 
	 * @param co
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void write(final RpcCall co) throws IOException, InterruptedException {
		// log.debug("Put RpcCall {} out", co);
		// 先将RPC请求或响应对象序列化成字节数组
		byte[] oba = RpcSerializer.serialize(co);
		if ((oba == null) || (oba.length < 1)) {
			throw new IllegalArgumentException("Byte array is invalid.");
		}
		// 根据最大分包大小计算分包数（不可能是0），缓冲区大小为最大包大小的N倍，一个缓冲区可以放多个分包
		int pc = (oba.length + RpcConfig.PACKAGE_BODY_MAX_SIZE - 1) / RpcConfig.PACKAGE_BODY_MAX_SIZE;
		// 取一个空缓冲区，如果没有则阻塞等待
		ByteBuffer buf = bufQueue.take();
		// 将包0的包头信息写入此缓冲区
		this.putHead(buf, co, 0, RpcConfig.PACKAGE_BODY0_SIZE);
		// 写入包0的包体内容，依次是：数据长度、分包数、协议类型
		buf.putInt(oba.length);
		buf.putInt(pc);
		buf.putInt(co.getProtocol());

		// 分包序号，表示正在写入第几个包
		int p = 1;
		// 已写入成功的字节数
		int i = 0;
		// 注意：当只有一个分包时，是不进这个for的，且最后一个分包也不在此for中处理
		for (; p < pc; p++) {
			// 如果有不止一个分包，且最后一个分包不由此for处理，则当前分包必然是最大尺寸的
			if (buf.capacity() - buf.position() < RpcConfig.PACKAGE_MAX_SIZE) {
				// 如果当前缓冲区不足以放下当前分包（最大尺寸），则将当前缓冲区放入发送队列
				buf.flip();
				this.outQueue.add(buf);
				// 再从空缓冲区队列抢一个空的缓冲区
				buf = bufQueue.take();
			}
			// 写入包头，包体尺寸为分包的最大大小
			this.putHead(buf, co, p, RpcConfig.PACKAGE_BODY_MAX_SIZE);
			// 将字节数组中从i到PACKAGE_BODY_MAX_SIZE区间的字节写入缓冲区buf
			buf.put(oba, i, RpcConfig.PACKAGE_BODY_MAX_SIZE);
			// 更新已写入的字节数
			i += RpcConfig.PACKAGE_BODY_MAX_SIZE;
		}
		// 如果还有字节剩余（处理唯一的一个分包或最后一个分包）
		if (oba.length > i) {
			// 计算剩余的字节数
			int r = oba.length - i;
			// 看当前缓冲区的剩余空间是否足够入下这些字节
			if (buf.capacity() - buf.position() < RpcConfig.PACKAGE_HEAD_SIZE + r) {
				// 如果当前缓冲区的剩余空间不足，则将当前缓冲区放入发送队列
				buf.flip();
				this.outQueue.add(buf);
				// 再从空缓冲区队列抢一个空的缓冲区
				buf = bufQueue.take();
			}
			// 写入最后一个或者唯一的一个包的包头
			this.putHead(buf, co, p, r);
			// 写入包体
			buf.put(oba, i, r);
		}
		// 将最后一个缓冲区放入发送队列
		buf.flip();
		this.outQueue.add(buf);
		// log.debug("Put RpcCall {} out successfully", co);
	}

	/**
	 * 将包头信息写入缓冲区
	 * 
	 * @param buf
	 *            要写入的缓冲区
	 * @param co
	 *            要发送的对象（从中获取要写入包头的部份信息）
	 * @param index
	 *            包索引，这是第几个包
	 * @param size
	 *            包体的大小
	 */
	protected void putHead(ByteBuffer buf, RpcCall co, int index, int size) {
		buf.putInt(co.getType()); // 请求还是响应标识
		buf.putInt(co.getMethodId()); // 方法ID
		buf.putLong(co.getMethodExeNo()); // 方法执行序号
		buf.putInt(index);// 第几个包
		buf.putInt(size);// 有多大
	}

	public void close(String reason) {
		switch (status.get()) {
		case STATUS_CLOSING:
		case STATUS_CLOSED:
			return;
		}
		log.info("Close RpcChannel {} because {}", key, reason);
		this.status.set(STATUS_CLOSING);
		try {
			// 中断读写线程
			this.reader.close();
			this.writer.close();
			// 关闭socket通道
			SocketUtil.closeChannel(this.asyncSocketChannel);
		} finally {
			this.status.set(STATUS_CLOSED);
			// 清除缓冲区队列
			this.bufQueue.clear();//
			this.outQueue.clear();//
			// 触发通道关闭事件
			this.rpcCore.onChannelClose(this, reason);
		}
	}

	public AsynchronousSocketChannel getAsyncSocketChannel() {
		return asyncSocketChannel;
	}

	public BlockingQueue<ByteBuffer> getOutQueue() {
		return outQueue;
	}

	public BlockingQueue<ByteBuffer> getBufQueue() {
		return bufQueue;
	}

	public String getKey() {
		return key;
	}

	public int getStatus() {
		return status.get();
	}

	public RpcLink getRpcLink() {
		return rpcLink;
	}

	public void setRpcLink(RpcLink rpcLink) {
		this.rpcLink = rpcLink;
	}

	public void setStatus(int status) {
		this.status.set(status);
	}

	public long getCreateTime() {
		return createTime;
	}

	public String getRemoteAddr() {
		return this.remoteAddr;
	}

	public void setRemoteAddr(String addr) {
		this.remoteAddr = addr;
	}
}
