package com.boarsoft.rpc.demo;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.boarsoft.common.Util;
import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.core.RpcCore;
import com.boarsoft.rpc.http.tomcat.TomcatServer;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Throwable {
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

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					RpcCore.getCurrentInstance().shutdown();
				} finally {
					ctx.close();
				}
			}
		});

		try {
//			RpcReferenceConfig rc = new RpcReferenceConfig("demo", "demo1", //
//					"com.boarsoft.rpc.demo.DemoService", "1.0.0", "demoService");
//			rc.setTimeout(6000);
//			rc.setMocker("demoMocker");
//			RpcCore.getCurrentInstance().registReferece(rc);
//			DemoService ds = ctx.getBean("demoService", DemoService.class);
//			Object ro = ds.helloSC(new User("Mac_J"));
//			log.info("Result = {}", ro);
			
//			TomcatServer ts = ctx.getBean(TomcatServer.class);
//			ts.getTomcat().getServer().await();
		} finally {
			// ctx.close();
		}
		// System.exit(0);
	}

}
