package com.boarsoft.rpc.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.proxy.InvocationHandler;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.spy.RpcSvcSpy;
import com.boarsoft.soagov.config.SvcConfig;
import com.boarsoft.soagov.spy.SpyData;

public class RpcInvoker implements InvocationHandler {
	private static final Logger log = LoggerFactory.getLogger(RpcInvoker.class);

	protected RpcCore rpcCore;
	protected RpcContext rpcContext;
	protected RpcReferenceConfig referenceConfig;
	/** RPC调用模拟器 */
	protected Object mocker;
	/** 是否开启模拟器 */
	protected RpcSvcSpy svcSpy;

	public String toString() {
		return this.referenceConfig.toString();
	}

	public RpcInvoker(RpcReferenceConfig referenceConfig, RpcCore rpcCore, RpcContext rpcContext)
			throws ClassNotFoundException {
		this.referenceConfig = referenceConfig;
		this.rpcCore = rpcCore;
		this.rpcContext = rpcContext;
		//
		String mn = referenceConfig.getMocker();
		if (Util.strIsEmpty(mn)) {
			return;
		}
		if (rpcContext.containsBean(mn)) {
			mocker = rpcContext.getBean(mn, referenceConfig.getInterfaceClazz());
		}
	}

	@Override
	public Object invoke(Object caller, Method method, Object[] args) throws Throwable {
		// log.debug("Intercept invocation {} -> {}", caller, method);
		RpcMethodConfig mc = this.referenceConfig.getMethodConfig(method);
		if (mc == null) {
			// 如果没有找到相应的服务方法配置，表示这只是一个普通的本地方法
			try {
				// 此处不能传caller，会导致造成方法调用的反复拦截，形成死循环（递归）调用
				return method.invoke(this, args);
			} catch (Exception e) {
				log.error("Can not find reference config of method {} on caller {}", method, caller.getClass().getName());
				throw new IllegalStateException("Only specified interface method can be invoked remotely.");
			}
		}
		// 一旦开始停机，所有调用在发起时就报错
		if (!RpcCore.isRunning()) {
			throw new RejectedExecutionException("I'm closing");
		}
		// 服务治理埋点，如果svcSpy不为空，且服务状态为关闭，则尝试使用mocker
		if (svcSpy != null) {
			String key = mc.getKey();
			SpyData sd = svcSpy.getSpyData(key);
			int status = svcSpy.checkStatus(sd);
			// 检查服务的开关状态，与是否开启了结果模拟
			if (status == SvcConfig.STATUS_DISABLE) {
				// 如果服务被关闭，直接抛出拒绝执行异常
				throw new RejectedExecutionException(key);
			} else if (status == SvcConfig.STATUS_MOCKING) {
				// 如果开启了结果模拟功能，通过服务治理插件调用模拟器，返回模拟的结果
				return svcSpy.mock(mocker, key, method, args);
			}
		}
		// 上面的服务治理检查通过后，才直接开始发送RPC请求
		final String key = this.referenceConfig.getSign();
		// 根据方法调用类型配置，确定处理方式
		switch (mc.getType()) {
		case RpcMethodConfig.TYPE_SYNC_BROADCAST: // 同步广播
		case RpcMethodConfig.TYPE_ASYNC_BROADCAST: // 异步广播
		case RpcMethodConfig.TYPE_BROADCAST_NOTICE: // 广播通知
			return this.broadcast(key, mc, args);
		case RpcMethodConfig.TYPE_ASYNC_CALL: // 异步方法调用
		case RpcMethodConfig.TYPE_SYNC_CALL: // 同步方法调用
		case RpcMethodConfig.TYPE_SYNC_NOTICE: // 同步通知
		case RpcMethodConfig.TYPE_ASYNC_NOTICE: // 异步通知
			return this.invoke(key, mc, args, method);
		}
		throw new IllegalStateException(String.format("Unknow method type %d", mc.getType()));
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> broadcast(String key, RpcMethodConfig mc, Object[] args) throws Throwable {
		// 声明一个map来装每个服务提供者的返回值
		Map<String, Object> rm = new HashMap<String, Object>();
		// 根据服务的key，获取此服务已知的服务提供者
		List<String> addrLt = rpcContext.getProvidersByKey(key);
		// 找不到服务提供者，则打印一个警告，并返回空map
		if (addrLt == null) {
			log.warn("No provider for method {} of referece {}", mc, key);
			return rm;
		}
		// 遍历所有服务提供者，获取到这些节点的连接，然后调用其方法
		for (String addr : addrLt) {
			log.info("Broadcast {} to {}", mc, addr);
			RpcLink lo = null;
			try {
				lo = this.rpcCore.link2(addr);
			} catch (Exception e) {
				log.info("Can not get link of {}", addr, e);
			}
			if (lo != null && lo.isConnected()) {
				try {
					// 不管是同步广播还是异步广播，总是以异步方式处理，返回Future
					Future<Object> ro = (Future<Object>) lo.invoke(mc, args);
					// 先将获得的Future对象放到map中
					rm.put(addr, ro);
				} catch (Throwable e) {
					log.error("Error on invoke method {} of {}", mc, addr, e);
				}
			}
		}
		// 如果是异步广播，直接向调用者返回装满Future对象的map即可
		if (mc.getType() == RpcMethodConfig.TYPE_ASYNC_BROADCAST) {
			return rm;
		}
		// 如果是同步广播，则需要调用这些Future对象的get方法，并用得到的结果替换map中的Future对象
		long start = System.currentTimeMillis();
		long timeout = 0L;
		for (String k : rm.keySet()) {
			// 用总的超时时间，减去已逝去的时间，计算出还可以等待的时间
			Future<Object> ft = (Future<Object>) rm.get(k);
			timeout = System.currentTimeMillis() - start;
			timeout = mc.getTimeout() - timeout;
			// timeout<=0 表示不等待，没取到就抛出TimeoutException
			// timeout = Math.max(0L, timeout);
			// 这里得到的对象可能是真实的结果，也可能是一个异常或者null
			Object v = ft.get(timeout, TimeUnit.MILLISECONDS);
			rm.put(k, v); // 替换现有的Future对象
		}
		return rm;
	}

	protected Object invoke(String key, RpcMethodConfig mc, Object[] args, Method method) throws Throwable {
		int i = 0;
		// 失败重试（失败转移）
		do {
			// 根据远程服务方法引用的key查找相应的服务提供者，并从它们中随机选取 一个
			String addr = this.rpcContext.getProvider(key);
			if (addr == null) {
				// 如果没有相应的服务提供者，且没有配置模拟器并开启自动模拟，直接抛出异常
				String s = "No provider available for ".concat(key);
				if (mocker == null || svcSpy == null || !mc.isAutoMock()) {
					throw new IllegalStateException(s);
				}
				log.warn("{}, call mocker {} instead", s, mc.getMocker());
				return svcSpy.mock(mocker, key, method, args);
			}
			// 获取到服务提供者的逻辑连接，并进行建立实际的socket连接，但不提交注册表
			RpcLink lo = null;
			try {
				lo = this.rpcCore.link2(addr);
			} catch (Exception e) {
				log.info("Can not get link of {}", addr, e);
			}
			// 如果连接可用，则在此连接上发送RPC调用请求
			if (lo != null && lo.isConnected()) {
				try {
					return lo.invoke(mc, args);
				} catch (RejectedExecutionException e) {
					log.warn("RPC invoking {} be rejected by {}", key, addr);
				}
			} else {
				// 如果这个节点连不通，应同时从providerMap和linkMap中移除
				log.warn("Link {} is null or disconnected", addr, key);
				// 移除RpcLink时会移除provider
				this.rpcCore.removeLink(addr, true, "it is broken");
			}
			i++;
			log.warn("Retry RPC invoking {} at {}", key, i);
		} while (i < mc.getFailover());
		// 三次调用失败后，尝试模拟器（如果有配置）
		String s = String.format("Invoke %s failed after failover %d", //
				mc.toString(), mc.getFailover());
		if (mocker == null || svcSpy == null || !mc.isAutoMock()) {
			throw new IllegalStateException(s);
		}
		log.warn("{}, call mocker {} instead", s, mc.getMocker());
		return svcSpy.mock(mocker, key, method, args);
	}

	public RpcSvcSpy getSvcSpy() {
		return svcSpy;
	}

	public void setSvcSpy(RpcSvcSpy svcSpy) {
		this.svcSpy = svcSpy;
	}
}
