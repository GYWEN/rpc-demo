package com.boarsoft.rpc.demo;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.boarsoft.common.Util;
import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.core.RpcCore;
import com.boarsoft.rpc.serialize.RpcSerializer;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

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

		final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:conf/context.xml");

		DemoService ds = ctx.getBean("demoService", DemoService.class);
		System.out.println(ds.getClass().getName());
		System.out.println(ds.getClass().getMethod("helloSC", User.class).toString());

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					RpcCore.getCurrentInstance().shutdown();
				} finally {
					ctx.close();
				}
			}
		});

		User o = new User("Mac_J");
		RpcCall c = new RpcCall();
		c.setArguments(new Object[] { o, 10, 1000 });
		byte[] b = RpcSerializer.serialize(c);
		System.out.println(b.length);

		// RpcSvcSpy svcSpy = ctx.getBean("rpcSvcSpy", RpcSvcSpy.class);
		// String mk =
		// "demo/demo1/com.boarsoft.rpc.sample.DemoService/1.0.0/helloSC(com.boarsoft.rpc.sample.User)";
		// svcSpy.down(mk, true);
		// svcSpy.setResult(mk, "service mock result");

		log.info("Startup successfully.");
	}
}
