package com.boarsoft.rpc.bean;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.boarsoft.rpc.core.RpcContext;

/**
 * 用于封装RPC调用的对象<br>
 * 注：Future<Object>的get可能得到RpcCall中的result或throwable
 * 
 * @author Mac_J
 *
 */
public class RpcInvoking implements Serializable, Future<Object> {
	private static final long serialVersionUID = 5499784854506943803L;

	/** 代表一次调用，remoteHost_methodId_methodExeNo */
	protected String key;
	/** 执行此调用的远程节点地址 */
	protected String remoteHost;
	/** 方法（引用）配置 */
	protected RpcMethodConfig methodConfig;
	/** RPC请求对象 */
	protected RpcCall rpcCall;
	/** 调用发起时间 */
	protected long startTime = System.currentTimeMillis();
	/** 调用完成时间 */
	protected volatile long endTime = 0L;
	/** 用于移除RpcInvoking */
	protected RpcContext rpcContext;

	/** 用于实现同步和控制超时 */
	protected Lock lock = new ReentrantLock();
	protected Condition condition = lock.newCondition();

	public RpcInvoking(RpcContext rpcContext, String remoteHost, RpcMethodConfig methodConfig, RpcCall rpcCall) {
		this.rpcContext = rpcContext;
		this.remoteHost = remoteHost;
		this.methodConfig = methodConfig;
		this.rpcCall = rpcCall;
		this.key = makeKey(remoteHost, rpcCall);
	}

	public static String makeKey(String remoteHost, final RpcCall co) {
		// 根据远程节点（方法ID是相对于远程节点的、方法ID、方法调用序号来唯一标识一个调用
		return new StringBuilder().append(remoteHost).append("_")//
				.append(co.getMethodId()).append("_").append(co.getMethodExeNo()).toString();
	}

	@Override
	public boolean isCancelled() {
		return false; // 不支持取消操作
	}

	@Override
	public boolean isDone() {
		return endTime > 0L; // Future完成的状态
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
//		synchronized (this) {
//			endTime = System.currentTimeMillis();
//			this.notifyAll();
//		}
		lock.lock();
		try {
			endTime = System.currentTimeMillis();
			condition.signalAll();
		} finally {
			lock.unlock();
		}
		return true;
	}

	public void complete() {
//		synchronized (this) {
//			endTime = System.currentTimeMillis();
//			this.notifyAll();
//		}
		lock.lock();
		try {
			endTime = System.currentTimeMillis();
			condition.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void hold(long timeout) throws InterruptedException {
//		synchronized (this) {
//			// 此时，当前调用可能已经返回了结果，在await前需检查一下
//			if (endTime > 0L) {
//				return;
//			}
//			// await被signal/signalAll唤醒时会返回true
//			this.wait(timeout);
//			if (endTime == 0L) {
//				endTime = System.currentTimeMillis();
//				rpcCall.setThrowable(new TimeoutException(String.format(//
//						"Rpc method %s timeout after %dms on %s", //
//						new Object[] { methodConfig.getKey(), this.getElapsedTime(), remoteHost })));
//			}
//		}
		lock.lock();
		try {
			// 此时，当前调用可能已经返回了结果，在await前需检查一下
			if (endTime > 0L) {
				return;
			}
			// await被signal/signalAll唤醒时会返回true
			if (condition.await(timeout, TimeUnit.MILLISECONDS)) {
				return;
			}
		} finally {
			lock.unlock();
		}
		// 正常结束和取消时，endTime都有值。超时结束的则没有
		endTime = System.currentTimeMillis();
		rpcCall.setThrowable(new TimeoutException(String.format(//
				"Rpc method %s timeout after %dms on %s", //
				new Object[] { methodConfig.getKey(), this.getElapsedTime(), remoteHost })));
	}

	@Override
	public Object get() throws ExecutionException {
		try {
			this.hold(methodConfig.getTimeout());
		} catch (InterruptedException e) {
			rpcCall.setThrowable(e);
		} finally {
			rpcContext.removeInvoking(this);
		}
		return rpcCall.getThrowable() == null ? //
				rpcCall.getResult() : rpcCall.getThrowable();
	}

	@Override
	public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		try {
			this.hold(unit.toMillis(timeout));
		} finally {
			rpcContext.removeInvoking(this);
		}
		return rpcCall.getThrowable() == null ? //
				rpcCall.getResult() : rpcCall.getThrowable();
	}

	public long getElapsedTime() {
		return endTime - startTime;
	}

	public String toString() {
		return this.key;
	}

	public boolean isTimeout() {
		return System.currentTimeMillis() > (startTime + methodConfig.getTimeout());
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public RpcMethodConfig getMethodConfig() {
		return this.methodConfig;
	}

	public RpcCall getRpcCall() {
		return this.rpcCall;
	}

	public String getRemoteHost() {
		return this.remoteHost;
	}

	public String getKey() {
		return this.key;
	}

	public void setRpcCall(RpcCall rpcCall) {
		this.rpcCall = rpcCall;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getEndTime() {
		return endTime;
	}
}
