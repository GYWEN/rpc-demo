package com.boarsoft.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;

/**
 * 自定义线程池，支持：任务（线程）分组、任务队列、任务（线程）取消（单个或分组取消）、任务（线程）优先级
 * 
 * @author Mac_J
 * 
 */
public class SimpleThreadPool implements ExecutorService, Runnable {
	private static final Logger log = LoggerFactory.getLogger(SimpleThreadPool.class);

	public static final short STATUS_RUNNING = 0;
	public static final short STATUS_SHUTTING_DOWN = 1;//
	public static final short STATUS_TERMINATED = 2;//
	public static final short STATUS_SHUTDOWN = 3;//

	/** 要维持的最小线程数，也是初始线程数，初始都放在idleThreads中 */
	protected int minSize = 20;
	/** 允许并发的最大线程数 */
	protected int maxSize = 100;
	/** 线程空闲（自旋）的最大时长，超过将被释放 */
	protected long maxIdle = 1000L;
	/** 线程池关闭时的最大等待时间 */
	protected int awaitTerminationSeconds = 0;

	/** */
	protected volatile short status = STATUS_RUNNING;

	/** 用于存放等待执行的任务，为保正线程安全，需要同步 */
	protected List<BlockingQueue<SimpleTask>> taskQueues = new ArrayList<BlockingQueue<SimpleTask>>();
	/** 所有线程，包括工作中的线程和空闲线程 */
	protected Map<Long, SimpleThread> allThreads = new ConcurrentHashMap<Long, SimpleThread>();
	/** 空闲线程 */
	protected BlockingQueue<SimpleThread> idleThreads;
	/** */
	protected Thread thread = new Thread(this);

	protected Lock lock = new ReentrantLock();
	protected Condition condition = lock.newCondition();
	protected volatile boolean await = false;

	@PostConstruct
	public void init() {
		if (maxSize < minSize) {
			throw new IllegalArgumentException("maxSize must > minSize");
		}
		// 初始化空闲队列
		if (minSize < 1) {
			throw new IllegalArgumentException("minSize must > 0");
		}
		idleThreads = new LinkedBlockingQueue<SimpleThread>(minSize);
		// 初始化执行线程池到minSize
		for (int i = 0; i < minSize; i++) {
			SimpleThread t = new SimpleThread(this);
			idleThreads.add(t);
			t.start();
		}
		// 初始化工作队列
		for (int i = 1; i <= 10; i++) {
			taskQueues.add(new LinkedBlockingQueue<SimpleTask>());
		}
		log.info("Simple thread pool be initiated");
		log.debug("minSize: {}, maxSize: {}, maxIdle: {}ms", minSize, maxSize, maxIdle);
		thread.start();
	}

	@Override
	public void run() {
		boolean noMore = true;
		while (true) {
			// 遍历所有的queue，按各自的优先级读取特定数量的任务分包
			for (int i = 1; i <= 10; i++) {
				BlockingQueue<SimpleTask> qu = taskQueues.get(i - 1);
				for (int p = 0; p < i; p++) {
					SimpleTask r = qu.poll();
					if (r == null) {
						continue;
					}
					log.debug("Got task {}", r);
					SimpleThread t = idleThreads.poll();
					if (t == null) {
						// 如果没有空闲线程可用，尝试创建新的工作线程
						synchronized (allThreads) {
							if (allThreads.size() < maxSize) {
								t = new SimpleThread(this);
								allThreads.put(t.getId(), t);
								t.setTask(r);
								t.start();
								continue;
							}
						}
						// 如果线程池已满，则等待空闲线程
						while (t == null) {
							try {
								t = idleThreads.take();
							} catch (InterruptedException e) {
								log.error("Be interrupted while take idle thread", e);
							}
						}
					}
					t.setTask(r);
				}
				noMore = noMore && qu.isEmpty();
			}
			if (noMore) {
				this.await(taskQueues);// 此方法会再次判断
			}
		}
	}

	protected void await(List<BlockingQueue<SimpleTask>> taskQueues) {
		lock.lock();
		try {
			for (BlockingQueue<SimpleTask> qu : taskQueues) {
				if (!qu.isEmpty()) {
					return;
				}
			}
			await = true;
			try {
				condition.await(1000L, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				log.error("Be interrupted", e);
				await = false;
			}
		} finally {
			lock.unlock();
		}
	}

	protected boolean idle(SimpleThread t) {
		return idleThreads.offer(t);
	}

	protected void free(SimpleThread t) {
		allThreads.remove(t.getId());
	}

	/**
	 * 
	 * @param tag
	 *            要取消的任务的路径
	 * @return
	 */
	public boolean cancel(String key) {
		log.info("Try interrupt thread with key {}", key);
		if (Util.strIsEmpty(key)) {
			throw new IllegalArgumentException("Invalid arguments.");
		}
		// 以 tag + / 打头的也应取消
		String p = key.concat("/");
		// 移除队列中的任务
		for (BlockingQueue<SimpleTask> qu : taskQueues) {
			synchronized (qu) {
				while (!qu.isEmpty()) {
					SimpleTask t = qu.peek();
					String k = t.getKey();
					if (Util.strIsEmpty(k)) {
						continue;
					}
					if (k.equals(key) || k.startsWith(p)) {
						qu.remove();
					}
				}
			}
		}
		// 中断正在执行的线程
		synchronized (allThreads) {
			for (SimpleThread ct : allThreads.values()) {
				String k = ct.getKey();
				if (Util.strIsEmpty(k)) {
					continue;
				}
				if (key.equals(k) || k.startsWith(p)) {
					ct.interrupt();
				}
			}
		}
		return true;
	}

	protected void addTask(SimpleTask task) {
		int priority = task.getPriority();
		BlockingQueue<SimpleTask> qu = taskQueues.get(priority);
		if (await) {
			lock.lock();
			try {
				qu.add(task);
				condition.signalAll();
			} finally {
				await = false;
				lock.unlock();
			}
		} else {
			qu.add(task);
		}
	}

	// ---------------

	@Override
	public void execute(Runnable task) {
		SimpleRunnable cr = null;
		if (task instanceof SimpleRunnable) {
			cr = (SimpleRunnable) task;
		} else {
			cr = new SimpleRunnable(task);
			cr.setKey(task.toString());
		}
		this.addTask(cr);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		SimpleCallable<T> cc = null;
		if (task instanceof SimpleCallable) {
			cc = (SimpleCallable<T>) task;
		} else {
			cc = new SimpleCallable<T>(task);
			cc.setKey(task.toString());
		}
		this.addTask(cc);
		return cc.getFuture();
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		SimpleRunnable cr = null;
		if (task instanceof SimpleRunnable) {
			cr = (SimpleRunnable) task;
		} else {
			cr = new SimpleRunnable(task);
			cr.setKey(task.toString());
		}
		int priority = cr.getPriority();
		taskQueues.get(priority).add(cr);
		SimpleFuture<T> ft = new SimpleFuture<T>();
		ft.setResult(result);
		return ft;
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.submit(task, null);
	}

	@Override
	public void shutdown() {
		log.warn("Simple thread pool gona shutdown");
		this.status = STATUS_SHUTTING_DOWN;
		try {
			this.awaitTermination(this.awaitTerminationSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.error("Be interrupted while await termination", e);
		} finally {
			this.shutdownNow();
		}
	}

	@Override
	public List<Runnable> shutdownNow() {
		log.warn("Simple thread tool shutdown now");
		try {
			// 移除队列中的任务
			for (BlockingQueue<SimpleTask> qu : taskQueues) {
				qu.clear();
			}
			// 中断正在执行的线程
			synchronized (allThreads) {
				for (SimpleThread ct : allThreads.values()) {
					ct.interrupt();
				}
				allThreads.clear();
			}
			// 中断空闲线程
			synchronized (idleThreads) {
				for (SimpleThread ct : idleThreads) {
					ct.end();
				}
				idleThreads.clear();
			}
			// TODO ？
			return null;
		} finally {
			this.status = STATUS_SHUTDOWN;
		}
	}

	@Override
	public boolean isShutdown() {
		return status == STATUS_SHUTDOWN;
	}

	@Override
	public boolean isTerminated() {
		return status == STATUS_TERMINATED;
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		boolean b = true;
		long last = System.currentTimeMillis();
		timeout = unit.toMillis(timeout);
		long elapse = 0L;
		while (elapse < timeout) {
			for (BlockingQueue<SimpleTask> qu : taskQueues) {
				if (!qu.isEmpty()) {
					b = false;
					break;
				}
			}
			if (b) {
				synchronized (allThreads) {
					for (SimpleThread ct : allThreads.values()) {
						if (ct.getTask() != null) {
							b = false;
							break;
						}
					}
				}
			}
			if (b) {
				status = STATUS_TERMINATED;
				return true;
			}
			elapse = System.currentTimeMillis() - last;
			// if (elapse < 1000L) {
			// Thread.sleep(1000L - elapse);
			// }
			last = System.currentTimeMillis();
		}
		return false;
	}

	@Override
	@Deprecated
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		if (tasks == null)
			throw new NullPointerException();
		List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
		boolean done = false;
		try {
			for (Callable<T> t : tasks) {
				Future<T> f = this.submit(t);
				futures.add(f);
			}
			for (Future<T> f : futures) {
				if (!f.isDone()) {
					try {
						f.get();
					} catch (CancellationException ignore) {
					} catch (ExecutionException ignore) {
					}
				}
			}
			done = true;
			return futures;
		} finally {
			if (!done)
				for (Future<T> f : futures)
					f.cancel(true);
		}
	}

	@Override
	@Deprecated
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		if (tasks == null || unit == null)
			throw new NullPointerException();
		long nanos = unit.toNanos(timeout);
		List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
		boolean done = false;
		try {
			for (Callable<T> t : tasks)
				futures.add(this.submit(t));

			long lastTime = System.nanoTime();

			for (Future<T> f : futures) {
				if (!f.isDone()) {
					if (nanos <= 0)
						return futures;
					try {
						f.get(nanos, TimeUnit.NANOSECONDS);
					} catch (CancellationException ignore) {
					} catch (ExecutionException ignore) {
					} catch (TimeoutException toe) {
						return futures;
					}
					long now = System.nanoTime();
					nanos -= now - lastTime;
					lastTime = now;
				}
			}
			done = true;
			return futures;
		} finally {
			if (!done)
				for (Future<T> f : futures)
					f.cancel(true);
		}
	}

	@Override
	@Deprecated
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		try {
			return doInvokeAny(tasks, false, 0);
		} catch (TimeoutException cannotHappen) {
			assert false;
			return null;
		}
	}

	@Override
	@Deprecated
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return doInvokeAny(tasks, true, unit.toNanos(timeout));
	}

	@Deprecated
	private <T> T doInvokeAny(Collection<? extends Callable<T>> tasks, boolean timed, long nanos)
			throws InterruptedException, ExecutionException, TimeoutException {
		if (tasks == null)
			throw new NullPointerException();
		int ntasks = tasks.size();
		if (ntasks == 0)
			throw new IllegalArgumentException();
		List<Future<T>> futures = new ArrayList<Future<T>>(ntasks);
		ExecutorCompletionService<T> ecs = new ExecutorCompletionService<T>(this);

		// For efficiency, especially in executors with limited
		// parallelism, check to see if previously submitted tasks are
		// done before submitting more of them. This interleaving
		// plus the exception mechanics account for messiness of main
		// loop.

		try {
			// Record exceptions so that if we fail to obtain any
			// result, we can throw the last exception we got.
			ExecutionException ee = null;
			long lastTime = timed ? System.nanoTime() : 0;
			Iterator<? extends Callable<T>> it = tasks.iterator();

			// Start one task for sure; the rest incrementally
			futures.add(ecs.submit(it.next()));
			--ntasks;
			int active = 1;

			for (;;) {
				Future<T> f = ecs.poll();
				if (f == null) {
					if (ntasks > 0) {
						--ntasks;
						futures.add(ecs.submit(it.next()));
						++active;
					} else if (active == 0)
						break;
					else if (timed) {
						f = ecs.poll(nanos, TimeUnit.NANOSECONDS);
						if (f == null)
							throw new TimeoutException();
						long now = System.nanoTime();
						nanos -= now - lastTime;
						lastTime = now;
					} else
						f = ecs.take();
				}
				if (f != null) {
					--active;
					try {
						return f.get();
					} catch (ExecutionException eex) {
						ee = eex;
					} catch (RuntimeException rex) {
						ee = new ExecutionException(rex);
					}
				}
			}

			if (ee == null)
				ee = new ExecutionException("", null);
			throw ee;

		} finally {
			for (Future<T> f : futures)
				f.cancel(true);
		}
	}

	// --------------

	public void setMinSize(int minSize) {
		if (minSize < 0) {
			throw new IllegalArgumentException("minSize must >= 0");
		}
		this.minSize = minSize;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		if (maxSize < 1) {
			throw new IllegalArgumentException("maxSize must > 0");
		}
		this.maxSize = maxSize;
	}

	public long getMaxIdle() {
		return maxIdle;
	}

	public void setMaxIdle(long maxIdle) {
		if (maxIdle < 0) {
			throw new IllegalArgumentException("maxIdle must >= 0");
		}
		this.maxIdle = maxIdle;
	}

	public int getMinSize() {
		return minSize;
	}
}