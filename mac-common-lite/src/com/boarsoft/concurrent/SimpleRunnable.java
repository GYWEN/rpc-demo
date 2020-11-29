package com.boarsoft.concurrent;

import java.util.Date;

public class SimpleRunnable extends SimpleTask implements Runnable {
	/** 要执行的任务对象，不可以为空 */
	protected Runnable runnable;
	
	protected SimpleFuture<Object> future = new SimpleFuture<Object>();
	
	public SimpleRunnable(Runnable r) {
		this.runnable = r;
	}

	@Override
	public void run() {
		startTime = new Date();
		runnable.run();
		endTime = new Date();
	}

	public Runnable getRunnable() {
		return runnable;
	}
}
