package com.boarsoft.concurrent;

import java.util.Date;
import java.util.concurrent.Callable;

public class SimpleCallable<T> extends SimpleTask implements Callable<T> {
	// private static Logger log =
	// LoggerFactory.getLogger(SimpleCallable.class);

	/** */
	protected Callable<T> callable;
	protected SimpleFuture<T> future = new SimpleFuture<T>();

	public SimpleCallable(Callable<T> c) {
		if (c == null) {
			throw new IllegalArgumentException("Callable parameter can not be null");
		}
		this.callable = c;
	}

	@Override
	public T call() throws Exception {
		startTime = new Date();
		try {
			T result = callable.call();
			future.setResult(result);
			return future.getResult();
		} catch (Exception e) {
			// log.error("Error on call {}", callable);
			future.setException(e);
			throw e;
		} finally {
			endTime = new Date();
		}
	}

	public Callable<T> getCallable() {
		return callable;
	}

	public SimpleFuture<T> getFuture() {
		return future;
	}
}
