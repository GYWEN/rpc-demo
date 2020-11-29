package com.boarsoft.concurrent;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleThread extends Thread {
	private static final Logger log = LoggerFactory.getLogger(SimpleThread.class);

	public static final short STATUS_IDLE = 0;
	public static final short STATUS_RUNNING = 1;

	protected volatile SimpleTask task;
	// protected final Lock lock = new ReentrantLock();
	// protected final Condition condition = lock.newCondition();
	protected SimpleThreadPool pool;
	protected Short status = STATUS_RUNNING;

	public SimpleThread(SimpleThreadPool pool) {
		this.pool = pool;
	}

	@Override
	public void run() {
		// lock.lock();
		try {
			while (true) {
				synchronized (this) {
					if (task == null) {
						// 如果前面有异常线程会中断退出，否则等待下一个runnable
						try {
							// condition.await(pool.getMaxIdle(),
							// TimeUnit.MILLISECONDS);
							this.wait(pool.getMaxIdle());
						} catch (InterruptedException e) {
							log.warn("{}/{} be interrupted while await new task", //
									this.getName(), this.getId());
						}
						// 如果超过指定还是没有拿到runnable，则检查是否退出还是继续空闲
						if (task == null) {
							// 如果已经是空闲状态则继续空闲
							if (status == STATUS_IDLE) {
								continue;
							}
							// 否则尝试加入空闲队列
							if (pool.idle(this)) {
								status = STATUS_IDLE;
								continue;
							} else {
								return; // finally中free
							}
						}
					}
					status = STATUS_RUNNING;
					log.debug("{}/{} gona run {}", this.getName(), this.getId(), task);
					try {
						if (task instanceof Runnable) {
							Runnable r = (Runnable) task;
							r.run();
						} else if (task instanceof Callable) {
							SimpleCallable<?> c = (SimpleCallable<?>) task;
							try {
								c.call();
								// } catch (InterruptedException e) {
								// log.warn("The call of {} be interrupted on
								// thread {}/{}", //
								// task, this.getId(), this.getName());
							} catch (Exception e) {
								log.error("Error on call task {}", task, e);
							}
						} else {
							throw new TypeNotPresentException(task.getClass().getName(), null);
						}
						log.debug("{}/{} completed {}", this.getName(), this.getId(), task);
					} finally {
						task = null;
					}
				}
			}
		} finally {
			// try {
			pool.free(this);
			// } finally {
			// lock.unlock();
			// }
		}
	}

	public void setTask(SimpleTask task) {
		// lock.lock();
		// try {
		synchronized (this) {
			this.task = task;
			this.notifyAll();
		}
		// condition.signalAll();
		// } finally {
		// lock.unlock();
		// }
	}

	public void end() {
		// lock.lock();
		// try {
		synchronized (this) {
			this.task = null;
		}
		// condition.signalAll();
		// } finally {
		// lock.unlock();
		// }
	}

	public String getKey() {
		if (task == null) {
			return null;
		}
		return task.getKey();
	}

	public SimpleTask getTask() {
		synchronized (this) {
			return task;
		}
	}
}