package com.boarsoft.soagov.spy;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class TpsBucket implements Serializable {
	
	private static final long serialVersionUID = 1925775124334545549L;
	protected int limit;
	protected AtomicInteger size;
	protected volatile int lastTps;

	public TpsBucket(int limit) {
		this.limit = limit;
		this.size = new AtomicInteger(limit);
	}

	/**
	 * 获取一个令牌（访问计数+1）
	 * 
	 * @return
	 */
	public boolean get() {
		int t = size.decrementAndGet();
		if (t > 0) {
			return true;
		}
		size.set(0);
		return false;
	}

	/**
	 * @return 当前TPS值 = TPS上限 - 剩余令牌数
	 */
	public int remain() {
		// return limit - Math.max(0, size.get());
		return limit - size.get();
	}

	public void reset() {
		lastTps = limit - size.get();
		size.set(limit);
	}

	public void limit(int limit) {
		this.limit = limit;
		this.size.set(limit);
	}

	public int limit() {
		return this.limit;
	}

	public int getLimit() {
		return limit;
	}

	public AtomicInteger getSize() {
		return size;
	}

	public int getLastTps() {
		return lastTps;
	}

	public void setLastTps(int lastTps) {
		this.lastTps = lastTps;
	}
	
}
