package com.boarsoft.rpc.center;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.boarsoft.common.Util;
import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.common.util.StreamUtil;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.core.RpcCore;

public class Main {
	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws NumberFormatException, IOException {
		log.info("Read conf/config.properties");
		// 读取自定义配置文件
		InputStream is = null;
		try {
			// 读取classpath:conf/config.properites
			ClassLoader cl = Main.class.getClassLoader();
			is = cl.getResourceAsStream("conf/config.properties");
			// 读取自定义的配置文件
			// is = new FileInputStream("d:/temp/config.properties");
			// 手工初始化RpcConfig
			RpcConfig.init(is);
		} catch (Exception e) {
			log.error("Failed to init Main:conf/config.properties", e);
			return;
		} finally {
			StreamUtil.close(is);
		}
		// 允许通过JVM启动参数或者程序参数两种方式来指定端口
		String port = System.getProperty("port");
		if (Util.strIsNotEmpty(port)) {
			RpcConfig.setAddr(InetConfig.LOCAL_IP, port);
		} else if (args.length > 0) {
			RpcConfig.setAddr(InetConfig.LOCAL_IP, args[0]);
		}
		// 启动容器，开始初始化
		final ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:conf/context.xml");
		// 添加停机钩子，以便在被kill时，关闭容器
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					RpcCore.getCurrentInstance().shutdown();
				} catch (Exception e) {
					log.error("Shutdown hook can not close application context", e);
				} finally {
					ctx.close();
				}
			}
		});
		System.out.println("Startup successfully.");
	}
}
