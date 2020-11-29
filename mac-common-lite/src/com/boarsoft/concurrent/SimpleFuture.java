package com.boarsoft.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SimpleFuture<T> implements Future<T> {
	protected T result;
	protected boolean done = false;
	protected Exception exception;

	@Override
	public synchronized T get() throws InterruptedException, ExecutionException {
		if (done) {
			return result;
		}
		this.wait();
		done = true;
		return result;
	}

	@Override
	public synchronized T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (done) {
			return result;
		}
		if (timeout < 1 || unit == null) {
			this.wait();
		} else {
			timeout = unit.toMillis(timeout);
			this.wait(timeout);
		}
		done = true;
		return result;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCancelled() {
		// TODO Auto-generated method stub
		return false;
	}

	public T getResult() {
		return result;
	}

	public synchronized void setResult(T result) {
		this.result = result;
		this.done = true;
		this.notifyAll();
	}

	public Exception getException() {
		return exception;
	}

	public void setException(Exception exception) {
		this.exception = exception;
	}
}
