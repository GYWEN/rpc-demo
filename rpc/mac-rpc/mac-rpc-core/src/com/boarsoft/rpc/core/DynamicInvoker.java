package com.boarsoft.rpc.core;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.ApplicationContext;

/**
 * 调用本地方法的标准接口，用于创建动态代理
 * 
 * @author Mac_J
 *
 */
public interface DynamicInvoker {
	/** 用于全局服务方法调用计数，调用发起前+1，调用完成后-1，也用于优雅停机 */
	public static final AtomicInteger count = new AtomicInteger(0);

	/**
	 * 调用本地服务方法
	 * 
	 * @param methodId
	 *            本地方法ID
	 * @param args
	 *            方法调用参数
	 * @return
	 * @throws Throwable
	 */
	public Object invoke(int methodId, Object[] args) throws Throwable;

	/**
	 * DynamicInvoker需要通过Spring容器获取要调用的本地bean<br>
	 * 因为在编译和创建这一动态代理时，要调用的本地bean还没被创建好
	 * 
	 * @param ctx
	 *            Spring容器
	 */
	public void setApplicationContext(ApplicationContext ctx);
}
