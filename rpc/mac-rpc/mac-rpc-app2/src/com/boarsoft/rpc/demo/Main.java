package com.boarsoft.rpc.demo;

import java.io.InputStream;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.boarsoft.common.Util;
import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.core.RpcContext;
import com.boarsoft.rpc.core.RpcCore;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	private static ExecutorService threadPool;

	private static int t_times = 10000;
	private static int t_sleep = 0;
	private static int t_chars = 5000;

	public static void main(String[] args) throws Exception {
		InputStream is = null;
		try {
			ClassLoader cl = Main.class.getClassLoader();
			is = cl.getResourceAsStream("conf/config.properties");
		} catch (Exception e) {
			log.error("Main.init failed with conf/config.properties", e);
			return;
		}
		try {
			RpcConfig.init(is);
		} catch (Exception e) {
			log.error("RpcConfig.init failed with conf/config.properties", e);
			return;
		}

		String port = System.getProperty("port");
		if (Util.strIsNotEmpty(port)) {
			RpcConfig.setAddr(InetConfig.LOCAL_IP, port);
		} else if (args.length > 0) {
			RpcConfig.setAddr(InetConfig.LOCAL_IP, args[0]);
		}

		if (args.length > 1) {
			t_times = Util.str2int(args[1], t_times);
			t_sleep = Util.str2int(args[2], t_sleep);
			t_chars = Util.str2int(args[3], t_chars);
		}

		final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:conf/context.xml");
		RpcCore rpcCore = ctx.getBean("rpcCore", RpcCore.class);
		threadPool = rpcCore.getThreadPool();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					RpcCore.getCurrentInstance().shutdown();
				} finally {
					ctx.close();
				}
			}
		});

		// RpcSvcSpy svcSpy = ctx.getBean("rpcSvcSpy", RpcSvcSpy.class);
		// String mk =
		// "demo/demo1/com.boarsoft.rpc.sample.DemoService/1.0.0/helloSC(com.boarsoft.rpc.sample.User)";
		// svcSpy.down(mk, true);
		// svcSpy.setResult(mk, "mock result");
		//
		try {
			final DemoService ds = (DemoService) ctx.getBean("demoService");
			for (int i = 0; i < 10000; i++) {
//				RpcContext.specify2("0");
				try {
					Object ro = ds.helloSC(new User("Mac_J"));
					log.info("Result = {}", ro);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
//					RpcContext.specify2(null);
				}
				// // Thread.sleep(10000L);
				// // User[] ua = new User[] { new User() };
				// // long[] w = new long[] { 1L, 2L };
				// // Integer[][] s = new Integer[][] {};
				// // String[] a = new String[] { "A", "B" };
				// // Object ro = ds.hello(ua, w, s, a);
				// // log.info("Result = {}", ro);
			}
			// basicTest1(ds); // 基本功能测试

			// Map<String, Object> rm = rpcCore.broadcast(99, new Object[]{ new
			// User("Mac_J") }, null);
			// log.info(JsonUtil.from(rm));

			// Thread.sleep(10000L);
			// log.info(ds.hello(new User("Mac_J"), 0, 5));
			// log.info(ds.hello(new User[3], new long[4], null));
			// ExecutorService es = (ExecutorService) ctx.getBean("threadPool");

//			scPerfTest1(ds);
			// scPerfTest2(ds);
			// acPerfTest1(ds);
			// acPerfTest2(ds, es);
		} finally {
			ctx.close();
		}
		System.exit(0);
	}

	public static void scPerfTest1(final DemoService ds) {
		final User u = new User("Mac_J");
		int tc = 300;
		// ExecutorService es = Executors.newFixedThreadPool(tc);
		ExecutorService es = threadPool;
		final CountDownLatch cdl = new CountDownLatch(tc);
		long l = System.currentTimeMillis();
		try {
			for (int n = 0; n < tc; n++) {
				es.execute(new Runnable() {
					public void run() {
						for (int i = 0; i < t_times; i++) {
							// log.info(ds.hello(u));
							ds.hello(u, t_sleep, t_chars);
							// try {
							// ds.hello(u, 0, 5);
							// log.debug("{}", i);
							// } catch (Exception e) {
							// log.error(e.getMessage());
							// try {
							// Thread.sleep(1000L);
							// } catch (InterruptedException e1) {
							// //e1.printStackTrace();
							// }
							// }
						}
						cdl.countDown();
					}
				});
			}
			cdl.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// es.shutdown();
		}
		System.out.println(System.currentTimeMillis() - l);
	}

	public static void scPerfTest2(final DemoService ds) {
		final User u = new User("Mac_J");
		int tc = 300;
		ExecutorService es = Executors.newFixedThreadPool(tc);
		final CountDownLatch cdl = new CountDownLatch(tc);
		long l = System.currentTimeMillis();
		try {
			for (int n = 0; n < tc; n++) {
				es.execute(new Runnable() {
					public void run() {
						for (int i = 0; i < 10000; i++) {
							try {
								ds.helloSC(u);
							} catch (Exception e) {

							}
						}
						cdl.countDown();
					}
				});
			}
			cdl.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			es.shutdown();
		}
		System.out.println(System.currentTimeMillis() - l);
	}

	public static void acPerfTest2(final DemoService ds, ExecutorService es) throws InterruptedException, ExecutionException {
		int tc = 300;
		final CountDownLatch cdl = new CountDownLatch(tc);
		long l = System.currentTimeMillis();
		final Vector<Future<String>> vector = new Vector<Future<String>>();
		try {
			for (int i = 0; i < tc; i++) {
				final int n = i;
				es.execute(new Runnable() {
					@SuppressWarnings("unchecked")
					public void run() {
						for (int i = 0; i < 2000; i++) {
							User u = new User(String.format("%d/%d", i, n));
							Future<String> ft = (Future<String>) ds.helloAC(u);
							vector.add(ft);
						}
						cdl.countDown();
					}
				});
			}
			cdl.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			es.shutdown();
		}
		for (Future<String> ft : vector) {
			// System.out.println(ft.get());
			ft.get();
		}
		System.out.println(System.currentTimeMillis() - l);
	}

	public static void acPerfTest1(final DemoService ds) throws InterruptedException, ExecutionException {
		final User u = new User("Mac_J");
		int tc = 300;
		ExecutorService es = Executors.newFixedThreadPool(tc);
		final CountDownLatch cdl = new CountDownLatch(tc);
		long l = System.currentTimeMillis();
		try {
			for (int n = 0; n < tc; n++) {
				es.execute(new Runnable() {
					@SuppressWarnings("unchecked")
					public void run() {
						for (int i = 0; i < 5000; i++) {
							Future<String> ft = (Future<String>) ds.helloAC(u);
							try {
								ft.get();
							} catch (InterruptedException | ExecutionException e) {
								e.printStackTrace();
							}
						}
						cdl.countDown();
					}
				});
			}
			cdl.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			es.shutdown();
		}
		System.out.println(System.currentTimeMillis() - l);
	}

	@SuppressWarnings("unchecked")
	public static void basicTest1(DemoService ds) throws InterruptedException, ExecutionException {
		// 从Spring容器中获取，也可以通过@Autowired等注解方式注入
		Object ro = null;
		Future<Object> ft = null;
		Map<String, Object> rm = null;

		ro = ds.helloSC(new User("Mac_J"));
		log.info("Result = {}", ro);

		// 同步通知，返回的是null（回声应答）
		ro = ds.helloSN(new User("Mac_J"));
		log.info("SN Result = {}", ro);

		try {
			// 同步通知有异常的情况
			ro = ds.helloSN(new User());
			log.info("SN Result = {}", ro);
		} catch (Exception e) {
			log.error("Error on SN invoking", e);
		}

		// 异步通知，返回的是future，get后得到null
		ft = (Future<Object>) ds.helloAN(new User("Mac_J"));
		log.info("AN Result = {}", ft.get());

		// 异步通知有异常的情况
		ft = (Future<Object>) ds.helloAN(new User());
		log.info("AN Result = {}", ft.get());

		// try {
		// // 此句会抛出来自服务提供者的异常
		// ro = ds.helloSC(new User());
		// log.info("Result = {}", ro);
		// } catch (Exception e) {
		// log.error("AC failed", e);
		// }

		RpcContext.specify2("127.0.0.1:9101");
		try {
			// 执行远程方法调用
			ro = ds.helloSC(new User("Mac_J"));
			log.info("Result = {}", ro);
		} finally {
			RpcContext.specify2(null);
		}

		ft = (Future<Object>) ds.helloAC(new User("Mac_J"));
		log.info("Do something before get actual result");
		ro = ft.get();
		log.info("Result = {}", ro);

		// 此句不会抛出异常，返回的结果是远程方法内部抛出的异常
		ft = (Future<Object>) ds.helloAC(new User());
		log.info("Do something before get actual result");
		ro = ft.get();
		// 此时ro对象为远程方法返回的异常
		log.info("Result = {}", ro);

		// 同步广播调用
		rm = (Map<String, Object>) ds.helloSB(new User("Mac_J"));
		for (String k : rm.keySet()) {
			ro = rm.get(k);
			log.info("{} = {}", k, ro);
		}
		// 此处是模拟服务方产生异常的情况
		rm = (Map<String, Object>) ds.helloSB(new User());
		for (String k : rm.keySet()) {
			// 返回的可能是null、异常、实际结果值
			ro = rm.get(k);
			log.info("{} = {}", k, ro);
		}
		// 异步广播调用
		rm = (Map<String, Object>) ds.helloAB(new User("Mac_J"));
		for (String k : rm.keySet()) {
			ro = rm.get(k);
			// 返回的可能是null、Future
			if (ro != null && ro instanceof Future) {
				// get返回的可能是异常，也可能是null
				ro = ((Future<Object>) ro).get();
			}
			log.info("{} = {}", k, ro);
		}

		// 广播通知
		rm = (Map<String, Object>) ds.helloBN(new User("Mac_J"));
		for (String k : rm.keySet()) {
			ro = rm.get(k);
			// 返回的可能是null、Future
			if (ro != null && ro instanceof Future) {
				// get返回的可能是异常，也可能是null
				ro = ((Future<Object>) ro).get();
			}
			log.info("{} = {}", k, ro);
		}

		// rm = (Map<String, Object>) ds.helloAB(new User());
		// for (String k : rm.keySet()) {
		// ro = rm.get(k);
		// if (ro != null && ro instanceof Future) {
		// ro = ((Future<Object>) ro).get();
		// }
		// log.info("{} = {}", k, ro);
		// }

	}
}
