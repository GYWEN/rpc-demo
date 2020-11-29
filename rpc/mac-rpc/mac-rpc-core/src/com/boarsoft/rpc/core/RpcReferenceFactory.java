package com.boarsoft.rpc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.Proxy;

import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.spy.RpcSvcSpy;

/**
 * 用于动态创建远程服务的本地引用（代理对象）
 * 
 * @author Mac_J
 *
 */
public class RpcReferenceFactory implements FactoryBean<Object> {
	private static final Logger log = LoggerFactory.getLogger(RpcReferenceFactory.class);
	/** 构造bean配置时手工赋值的 */
	protected RpcReferenceConfig referenceConfig;
	/** 构造bean配置时手工赋值的 */
	protected RpcCore rpcCore;
	/** 构造bean配置时手工赋值的 */
	protected RpcContext rpcContext;
	/** svcSpy可选 */
	protected RpcSvcSpy svcSpy;

	@Override
	public Object getObject() throws Exception {
		log.debug("Create proxy of {} for {}/{}/{}", referenceConfig.getId(), referenceConfig.getGroup(),
				referenceConfig.getInterfaceName(), referenceConfig.getVersion());
		// RpcInvoker实现了InvocationHandler，用于将代理对象的方法和转换为RPC请求并发送出去
		RpcInvoker invoker = new RpcInvoker(this.referenceConfig, this.rpcCore, this.rpcContext);
		invoker.setSvcSpy(svcSpy); // svcSpy可能是null
		// 根据远程服务接口创建代理对象，使用invoker拦截（实现）此代理对象的方法调用
		return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
				new Class[] { this.referenceConfig.getInterfaceClazz() }, invoker);
	}

	@Override
	public Class<?> getObjectType() {
		if (referenceConfig == null) {
			return null;
		}
		try {
			// 创建的代理是远程接口的实现类
			return referenceConfig.getInterfaceClazz();
		} catch (ClassNotFoundException e) {
			log.warn("Can not found class {} for {}", referenceConfig.getInterfaceName(), referenceConfig, e);
			return null;
		}
	}

	@Override
	public boolean isSingleton() {
		return true; // 单例
	}

	// getter / setter

	public RpcReferenceConfig getReferenceConfig() {
		return this.referenceConfig;
	}

	public void setReferenceConfig(RpcReferenceConfig referenceConfig) {
		this.referenceConfig = referenceConfig;
	}

	public RpcCore getRpcCore() {
		return this.rpcCore;
	}

	public void setRpcCore(RpcCore rpcCore) {
		this.rpcCore = rpcCore;
	}

	public RpcContext getRpcContext() {
		return this.rpcContext;
	}

	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public RpcSvcSpy getSvcSpy() {
		return svcSpy;
	}

	public void setSvcSpy(RpcSvcSpy svcSpy) {
		this.svcSpy = svcSpy;
	}
}
