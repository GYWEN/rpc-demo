package com.boarsoft.rpc.bean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存放每个远程节点上的服务方法ID，以及这些方法在本地的调用序号
 * 
 * @author Mac_J
 *
 */
public class RpcStub {
	/** 远程节点的地址，也是当前RpcStub的key */
	protected String remoteHost;
	/** 方法调用序号，与方法ID一起唯一的标识了一次调用 */
	protected final ConcurrentMap<Integer, AtomicLong> methodExeNoMap = new ConcurrentHashMap<Integer, AtomicLong>();
	/** 远程服务提供者的方法ID表，key为方法的KEY（接口签名 + 方法签名） */
	protected final Map<String, Integer> methodIdMap = new ConcurrentHashMap<String, Integer>();

	public RpcStub(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	/**
	 * 缓存远程节点（remoteHost）提供的服务方法（用KEY来唯一标识）的ID
	 * 
	 * @param methodKey
	 *            方法的KEY（接口签名 + 方法签名）
	 * @param methodId
	 *            方法ID
	 */
	public void putMethodId(String methodKey, Integer methodId) {
		this.methodIdMap.put(methodKey, methodId);
	}

	/**
	 * 获取指定方法的ID（远程节点中定义的ID）
	 * 
	 * @param methodKey
	 *            方法的KEY（接口签名 + 方法签名）
	 * @return
	 */
	public Integer getMethodId(String methodKey) {
		return this.methodIdMap.get(methodKey);
	}

	/**
	 * 根据方法ID获取方法的调用序号
	 * 
	 * @param methodId
	 * @return
	 */
	public long getMethodExeNo(int methodId) {
		AtomicLong no = (AtomicLong) this.methodExeNoMap.get(methodId);
		if (no == null) {
			synchronized (methodExeNoMap) {
				no = (AtomicLong) this.methodExeNoMap.get(methodId);
				if (no == null) {
					this.methodExeNoMap.put(methodId, new AtomicLong(1L));
					return 0L;
				}
			}
		}
		long l = no.getAndIncrement();
		if (l > Long.MAX_VALUE - 20000) {
			no.compareAndSet(l, 0);
			l = no.getAndIncrement();
		}
		return l;
	}

	public String getRemoteHost() {
		return this.remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}

	public ConcurrentMap<Integer, AtomicLong> getMethodExeNoMap() {
		return methodExeNoMap;
	}

	public Map<String, Integer> getMethodIdMap() {
		return methodIdMap;
	}
}
