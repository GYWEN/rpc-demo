package com.boarsoft.rpc.core;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.rpc.RpcConfig;

/**
 * 负责将发送队列中的缓冲区数据通过socket发送出去
 * 
 * @author Mac_J
 *
 */
public class RpcWriter extends Thread {
	private static final Logger log = LoggerFactory.getLogger(RpcWriter.class);

	/** 所属逻辑通道 */
	protected RpcChannel rpcChannel;
	/** 轮流用于合并发送队列中的缓冲区，和向socket通道写入的两个大缓冲区 */
	protected ByteBuffer bufferA = ByteBuffer.allocateDirect(RpcConfig.BUFFER_SIZE * RpcConfig.WRITER_BUFFERS);
	protected ByteBuffer bufferB = ByteBuffer.allocateDirect(RpcConfig.BUFFER_SIZE * RpcConfig.WRITER_BUFFERS);

	// 代表上一次发送（异步写入）的Future对象
	Future<Integer> lastFuture = null;

	public RpcWriter(RpcChannel rpcChannel) {
		this.rpcChannel = rpcChannel;
	}

//	public void run() {
//		// 获取asyncSocketChannel对象
//		AsynchronousSocketChannel asc = this.rpcChannel.getAsyncSocketChannel();
//		// 获取发送缓冲区队列
//		BlockingQueue<ByteBuffer> outQueue = this.rpcChannel.getOutQueue();
//		// 获取空缓冲区队列
//		BlockingQueue<ByteBuffer> bufQueue = this.rpcChannel.getBufQueue();
//		// 当前待发送缓冲区
//		ByteBuffer buf = null;
//
//		// 不断从缓冲区发送队列获取待么送的缓冲区，并发送
//		try {
//			// 这里不能判断rpcCore.isRunning，因为停机过程中可能还有通信
//			while (true) {
//				// 从缓冲区发送队列获取待么送的缓冲区
//				buf = outQueue.take();
//				// buffer = rpcChannel.takeOutBuffer();
//				do {
//					// 此write方法会调用AIO的异步写入方法，此调用会立即返回，而不需要等到数据被实际写入完成
//					asc.write(buf).get();
//					// 此get方法会一直阻塞直到上一笔数据被写入完成，并返回已成功写入的字节数
//				} while (buf.hasRemaining());
//				// 将用过的缓冲区归还空缓冲区队列
//				bufQueue.add((ByteBuffer) buf.clear());
//			}
//		} catch (Exception e) {
//			this.onException(e);
//			return;
//		}
//		// rpcChannel.close("Writer exit after connection broken");
//	}

	@Override
	public void run() {
		// 获取asyncSocketChannel对象
		AsynchronousSocketChannel asc = this.rpcChannel.getAsyncSocketChannel();
		// 获取发送缓冲区队列
		BlockingQueue<ByteBuffer> outQueue = this.rpcChannel.getOutQueue();
		// 获取空缓冲区队列
		BlockingQueue<ByteBuffer> bufQueue = this.rpcChannel.getBufQueue();

		// 局部变量，用于临时指向某个缓冲区
		ByteBuffer buf = null;
		// 已并入“大缓冲区”的“小缓冲区”数量
		int bufCt = 0;

		try {
			// while (!Thread.interrupted() && asc.isOpen()) {
			while (true) {
				// 首次发送与当前缓冲区内容发送完成时，lastFuture为空
				while (lastFuture != null) {
					// 判断上一次写操作（数据发送）是否完成
					while (!lastFuture.isDone() && bufCt < RpcConfig.WRITER_BUFFERS) {
						// 如果还没完成，则继续从外发缓冲区队列中获取一个缓冲区
						buf = outQueue.poll();
						if (buf != null) {
							// 如果有取到，就写到待发送的大缓冲区中
							bufferA.put(buf);
							// 将用过的缓冲区归还空缓冲区队列
							buf.clear();
							bufQueue.add(buf);
							// 已合并缓冲区计数加一
							bufCt++;
						}
					}
					// 这一句不能少，否则关闭socket通道时，会导致IO线程无法退出
					lastFuture.get();
					// 获取缓冲区中的剩余字节数，如果还有剩余，则继续发送剩余的字节
					if (bufferB.hasRemaining()) {
						// 继续发送上一次未发送完的数据，直到全部发送完成
						lastFuture = asc.write(bufferB);
					} else {
						bufferB.clear(); // 退空缓冲区以备下次写入
						lastFuture = null; // 退出循环
					}
				}
				// 在前面的基础上，继续装满bufferA
				while (bufCt < RpcConfig.WRITER_BUFFERS) {
					// 从外发缓冲区队列中获取一个缓冲区
					buf = outQueue.poll();
					if (buf == null) {
						break; // 没取到不取了
					}
					// 有取到，则合并到待发送的大缓冲区中
					bufferA.put(buf);
					// 将用过的缓冲区归还空缓冲区队列
					buf.clear();
					bufQueue.add(buf);
					// 已合并缓冲区计数加一
					bufCt++;
				}
				// 如果已合并至少一个缓冲区到待发送大缓冲区
				if (bufCt > 0) {
					bufCt = 0; // 清零，以备下一轮使用
				} else {
					// 无数据可发时，通过take转入阻塞状态
					buf = outQueue.take();
					// 一旦获取到就继续，但不立即发送，等一下生产者，再合着下一轮一起
					bufferA.put(buf);
					// 将用过的缓冲区归还空缓冲区队列
					buf.clear();
					bufQueue.add(buf);
					// 已合并缓冲区计数加一
					bufCt++;
					continue;
				}
				// 开始发送，交换两个缓冲区，分别用于缓冲区合并和数据发送
				buf = bufferB;
				bufferB = bufferA; // bufferB被清空并变成bufferA，用于缓冲区合并
				bufferA = buf; // bufferA则变成bufferB，用于发送
				bufferB.flip();
				// 此write方法会调用AIO的异步写入方法，此调用会立即返回，而不需要等到数据被实际写入完成
				lastFuture = asc.write(bufferB);
			}
		} catch (Exception e) {
			this.onException(e);
			return;
		}
		// rpcChannel.close("Writer exit after connection broken");
	}
	
	protected void onException(Exception e) {
		// 避免lastFuture不get导致IO线程不退出的问题
		this.getLastFuture();
		// 停机时，关闭socket通道导致的异常不需要打印
		if (RpcCore.isRunning() && RpcCore.hasLink(rpcChannel.getRemoteAddr())) {
			if (e instanceof InterruptedException) {
				log.warn("Be interrupted while write rpc channel {} on {}", //
						rpcChannel, rpcChannel.getRemoteAddr());
			} else {
				// 反之，一旦发现异常就关闭当前逻辑通道，触发连接断开事件
				log.error("Error on writer of rpc channel {} on {}", //
						rpcChannel, rpcChannel.getRemoteAddr(), e);
			}
		}
		rpcChannel.close(e.getMessage());
	}

	public void close() {
		log.info("Stop writer of RpcChannel {}", this.rpcChannel);
		// 如果在write和read过程中关闭socket通道，在较低内核版本的LINUX上会导致IO线程死循环
		this.getLastFuture();
		this.interrupt(); // 中断线程，退出循环
	}

	protected void getLastFuture() {
		if (lastFuture == null) {
			return;
		}
		try {
			lastFuture.get(RpcConfig.WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			log.error("Error while get last write future");
		} finally {
			lastFuture = null;
		}
	}
}
