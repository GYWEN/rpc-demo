package com.boarsoft.concurrent;

import java.util.Date;

public abstract class SimpleTask {
	/** 任务优先级，1－10，数字越大越优先 */
	protected int priority = 5;
	/** 任务标识，形如：xxxx/xxxx */
	protected String key;
	/** 任务开始时间 */
	protected Date startTime;
	/** 任务结束时间 */
	protected Date endTime;
	/** 任务超时时间，大于0的整数，单位毫秒，0（默认）表示不计时 */
	protected int timeout = 0;
	/** 单节点最大并发数 */
//	protected int maxThreads = 200;
	
	@Override
	public String toString(){
		return key;
	}
	
	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		if (priority < 1 || priority > 10) {
			throw new IllegalArgumentException("Thread priority must >= 1 and <= 10.");
		}
		this.priority = priority;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}
