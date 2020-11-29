package com.boarsoft.boar.context;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.boarsoft.rpc.core.RpcCore;

public class ServletContextListenerImpl implements ServletContextListener {
	private static final Logger log = LoggerFactory.getLogger(ServletContextListenerImpl.class);

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.warn("Servlet context to be destroyed now");
		// 关闭RpcCore，如果有声明的话
		RpcCore rpcCore = RpcCore.getCurrentInstance();
		if (rpcCore != null) {
			try {
				rpcCore.shutdown();
			} catch (Throwable e) {
				log.error("Error on shutdown RpcCore", e);
			}
		}
		// 获取Spring容器
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(sce.getServletContext());
		// 关闭前取出所有线程池，稍后强制关闭
		Map<String, ExecutorService> esMap = wac.getBeansOfType(ExecutorService.class);
		try {
			((Closeable) wac).close(); // 关闭Spring容器
		} catch (Throwable e) {
			log.error("Error on close application context", e);
		}
		// 强制关闭所有线程池（Spring容器只会调用这些线程池的shutdown方法，不会强制关闭
		for (ExecutorService es : esMap.values()) {
			try {
				es.shutdownNow(); // 强制关闭
			} catch (Throwable e) {
				log.error("Error on shutdown thread pools", e);
			}
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		// Nothing to do
	}
}
