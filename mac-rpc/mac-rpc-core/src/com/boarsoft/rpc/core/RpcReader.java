package com.boarsoft.rpc.core;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcPackage;

/**
 * Socket通道读取器
 * 
 * @author Mac_J
 *
 */
public class RpcReader extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RpcReader.class);

	public static final int FLAG_HEAD = 0;
	public static final int FLAG_BODY0 = 1;
	public static final int FLAG_BODY = 2;

	protected RpcChannel rpcChannel;

	// 缓冲区A用于存放已接收，且正在处理，或还未来得及处理的数据
	protected ByteBuffer bufferA = ByteBuffer.allocateDirect(RpcConfig.PACKAGE_MAX_SIZE * RpcConfig.READER_BUFFERS * 2);
	// 缓冲区B用于接收从socket通道中读取数据，也就是新接收的数据
	protected ByteBuffer bufferB = ByteBuffer.allocateDirect(RpcConfig.PACKAGE_MAX_SIZE * RpcConfig.READER_BUFFERS);

	// cacheMap用于指向下面两个缓存表引用
	protected Map<Integer, Map<Long, RpcPackage>> cacheMap = null;
	// RPC请求缓存数据包暂存表，第一层map的key为方法ID，第二层map的key为方法执行序号
	protected Map<Integer, Map<Long, RpcPackage>> requestDataMap = new HashMap<Integer, Map<Long, RpcPackage>>(600);
	// RPC响应缓存数据包暂存表，第一层map的key为方法ID，第二层map的key为方法执行序号
	protected Map<Integer, Map<Long, RpcPackage>> responseDataMap = new HashMap<Integer, Map<Long, RpcPackage>>(600);

	protected Future<Integer> lastFuture = null; // 代表上一次读取的Future

	public RpcReader(RpcChannel rpcChannel) {
		this.rpcChannel = rpcChannel;
	}

	@Override
	public void run() {
		// 获取asyncSocketChannel对象
		AsynchronousSocketChannel asc = this.rpcChannel.getAsyncSocketChannel();

		int received = 0; // 已接收字节点数
		int packageIndex = -1; // 这是第几个包
		int least = RpcConfig.PACKAGE_HEAD_SIZE; // 解析前最少需要的字节数
		int flag = FLAG_HEAD; // 代表接下来要读取的数据类型
		int bodyLength = -1; // 用来存放包体的长度
		int type = -1; // 数据包类型
		int methodId = -1; // 方法ID
		long methodExeNo = -1L; // 方法执行序号
		// int yieldCt = 0;

		try {
			// 先从socket通道读取数据到缓冲区B，确保lastFuture不为空
			lastFuture = asc.read(bufferB);
			// 这里不能判断rpcCore.isRunning，因为停机过程中可能还有通信
			// while (!Thread.interrupted() && asc.isOpen()) {
			while (true) {
				// 如果已接收的字节数少于所需要最低字节数，则继续读取
				while (received < least) {
					// 等到上一次读取操作的完成
					int r = lastFuture.get();
					// 如果有读到数据，则将读到的数据放入缓冲区A
					if (r > 0) {
						received += r; // 已接收且未处理的字节数
						if (received >= least) {
							bufferB.flip();
							bufferA.put(bufferB);
							bufferB.clear(); // 清空缓冲区B，准备下一次读取
						}
					} else {
						// 如果未读到有效数据则暂时让出CPU，重新竞争CPU并重试，避免占满CPU
						Thread.sleep(RpcConfig.READER_SLEEP);
					}
					// 从socket通道读取数据到缓冲区B
					lastFuture = asc.read(bufferB);
				}
				// 此时，缓冲区A中累积的数据已经足够执行一下次解析
				bufferA.flip();

				// 根据预期的数据类型，分别读取
				if (flag == FLAG_HEAD) {
					// 如果是包头，则按以下顺序读取
					type = bufferA.getInt();
					methodId = bufferA.getInt();
					methodExeNo = bufferA.getLong();
					packageIndex = bufferA.getInt();
					bodyLength = bufferA.getInt();
					// 接下来需要至少读取bodyLength个字节才继续处理
					least = bodyLength;
					// 如果是包0，接下来需要读取包0的包体
					if (packageIndex == 0) {
						flag = FLAG_BODY0; // 处理包0的包体
						// 如果是一个RPC请求，让cacheMap指定RPC请求缓存表
						if (type == RpcCall.TYPE_REQUEST) {
							cacheMap = requestDataMap;
						} else if (type == RpcCall.TYPE_RESPONSE) {
							cacheMap = responseDataMap;
						} else {
							throw new IllegalStateException("Unknown RPC type "//
									.concat(String.valueOf(type)));
						}
						// 如果此方法ID还没有相应的缓存表，则添加一个
						if (!cacheMap.containsKey(methodId)) {
							// 由于RpcReader是相对于RpcChannel的，因此方法ID不可能重复
							cacheMap.put(methodId, new HashMap<Long, RpcPackage>());
						}
					} else {
						flag = FLAG_BODY; // 否则要处理的是普通包体
					}
				} else if (flag == FLAG_BODY0) {
					// 如果正处理的是包0的包体，则按如下方式读取
					RpcPackage pd = new RpcPackage();
					int total = bufferA.getInt(); // 数据总长度
					pd.setTotalPackage(bufferA.getInt()); // 分包数
					pd.setProtocol(bufferA.getInt()); // 使用何种协议
					pd.setData(new byte[total]); // 根据总长度，创建对应大小的字节数组
					// 因为是包0，将创建的数据包放入缓存，以便将后续收到的数据放进来
					cacheMap.get(methodId).put(methodExeNo, pd);
					// 包0的包体处理 完后，接下来就需要读取下一个包（包头）
					least = RpcConfig.PACKAGE_HEAD_SIZE;
					flag = FLAG_HEAD;
				} else if (flag == FLAG_BODY) {
					// 如果正处理的是普通包体，则按下面的方式读取
					Map<Long, RpcPackage> m = cacheMap.get(methodId);
					// 取出之前处理包0时准备的数据包对象
					RpcPackage pd = m.get(methodExeNo);
					// 由于已接收了足够的，直接从缓冲区中复制到缓存（RpcPackage中的字节数组）中
					bufferA.get(pd.getData(), pd.getWriteIndex(), bodyLength);
					// 向后移动字节数组的写入索引，准备下一次复制
					pd.moveWriteIndex(bodyLength);
					// 包计数加一
					pd.plusPackageCount();
					// 如果已收到足够的分包，则表示当前RPC请求或响应对象的数据已经接收完整了
					if (pd.isCompleted()) {
						m.remove(methodExeNo); // 移除缓存
						rpcChannel.read(type, pd.getData()); // 处理收到的数据
					}
					// 接着读取下一组数据（也是从包头开始）
					least = RpcConfig.PACKAGE_HEAD_SIZE;
					flag = FLAG_HEAD;
				} else {
					throw new IllegalStateException("Unknown RPC flag "//
							.concat(String.valueOf(flag)));
				}
				// 更新已接收但未处理的字节数
				received = bufferA.remaining();
				// 将缓冲区A中未读取的数据移动到缓冲区头部，以便腾出空间来放自缓冲区B（新接收）的数据
				bufferA.compact();
			}
		} catch (Exception e) {
			this.onException(e);
			return;
		}
		// 如果因实际连接通道不可用要导致无异常情况下退出上面的循环，需要确保逻辑通道也被关闭
		// rpcChannel.close("Reader exit after connection broken");
	}

	protected void onException(Exception e) {
		// 避免lastFuture不get导致IO线程不退出的问题
		this.getLastFuture();
		// 停机时，关闭socket通道导致的异常不需要打印
		if (RpcCore.isRunning() && RpcCore.hasLink(rpcChannel.getRemoteAddr())) {
			if (e instanceof InterruptedException) {
				log.warn("Be interrupted while read rpc channel {} on {}", //
						rpcChannel, rpcChannel.getRemoteAddr());
			} else {
				// 反之，一旦发现异常就关闭当前逻辑通道，触发连接断开事件
				log.error("Error on reader of rpc channel {} on {}", //
						rpcChannel, rpcChannel.getRemoteAddr(), e);
			}
		}
		rpcChannel.close(e.getMessage());
	}

	public void close() {
		log.info("Stop reader of RpcChannel {}", this.rpcChannel);
		// 如果在write和read过程中关闭socket通道，在较低内核版本的LINUX上会导致IO线程死循环
		this.getLastFuture();
		this.interrupt(); // 中断线程，退出循环
	}

	protected void getLastFuture() {
		if (lastFuture != null) {
			try {
				lastFuture.get(RpcConfig.READ_TIMEOUT, TimeUnit.MILLISECONDS);
			} catch (Exception e) {
				log.warn("Error while get last write future on RPC channel {}", rpcChannel);
			}
		}
	}
}
