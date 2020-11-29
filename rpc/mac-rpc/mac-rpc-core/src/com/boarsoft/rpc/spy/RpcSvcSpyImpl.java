package com.boarsoft.rpc.spy;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcContext;
import com.boarsoft.soagov.spy.SimpleSvcSpyImpl;

public class RpcSvcSpyImpl extends SimpleSvcSpyImpl implements RpcSvcSpy {
	@Autowired
	protected RpcContext rpcContext;

	protected Map<String, MethodMocking> mockingMap = new ConcurrentHashMap<String, MethodMocking>();

	public RpcSvcSpyImpl() {
		this.reqReader = new RpcSvcReqReader();
	}

	@Override
	public Object getData(String mk) {
		// 此处key为方法粒度，需要先检查当前节点是否提供此接口服务，否则创建SpyData
		Map<String, RpcServiceConfig> sm = rpcContext.getLocalServiceConfigMap();
		String sk = this.getKey(mk);
		if (sm.containsKey(sk)) {
			return this.getSpyData(mk);
		}
		return null;
	}

	@Override
	public Object mock(Object mocker, String key, Method m, Object[] args) throws Exception {
		if (mocker == null) {
			return this.getResult(key);
		}
		// 以同样的接口去调用给定的模拟器
		m = mocker.getClass().getMethod(m.getName(), m.getParameterTypes());
		return m.invoke(mocker, args);
	}

	@Override
	public Object mock(Object input) {
		RpcSvcInput req = (RpcSvcInput) input;
		RpcCall co = req.getCall();
		RpcMethodConfig mc = req.getMethodConfig();
		return this.mock(co, mc);
	}

	@Override
	public Object mock(RpcCall co, RpcMethodConfig mc) {
		// RpcChannel rc = req.getChannel();
		// 当服务状态为关闭时，检查是否有配置有效的mocker
		String mn = mc.getMocker();
		// 没有就返回给定的假返回值，若没有指定就返回null
		if (Util.strIsEmpty(mn)) {
			Object o = this.getResult(mc.toString());
			co.setResult(o);
			return o;
		}
		// 如果有配置mocker则调用mocker的对应方法
		String key = mc.toString();
		// 先获取缓存的mocking（mocker及其method）
		MethodMocking mocking = null;
		if (mockingMap.containsKey(key)) {
			mocking = mockingMap.get(key);
		} else {
			synchronized (mockingMap) {
				if (mockingMap.containsKey(key)) {
					mocking = mockingMap.get(key);
				} else {
					if (rpcContext.containsBean(mn)) {
						try {
							Class<?> ic = mc.getFaceConfig().getInterfaceClazz();
							Object bo = rpcContext.getBean(mn, ic);
							// 获取原本要调用的目标对象的目标方法
							Method m = rpcContext.getMyMethod(co.getMethodId());
							// 根据要调用的方法信息，查找模拟器中对应的方法
							m = bo.getClass().getMethod(m.getName(), m.getParameterTypes());
							mocking = new MethodMocking(key, bo, m);
							mockingMap.put(key, mocking);
						} catch (Exception e) {
							log.error("Error on call mocker method: {}.{}", mn, mc, e);
							co.setThrowable(e);
							return null;
						}
					}
				}
			}
		}
		// 如果bean不存在，或者调用过程中发生异常，都返回null
		try {
			Object result = mocking.invoke(co.getArguments());
			co.setResult(result);
			return result;
		} catch (Exception e) {
			log.error("Error on invoking mock method {}", mocking, e);
			co.setThrowable(e);
		}
		return null;
	}

	@Override
	public Object disableProvider(String addr, String mk) {
		log.warn("Disable provider {} for service {}", addr, mk);
		rpcContext.disableProvider(addr, this.getKey(mk));
		return true;
	}
	
	@Override
	public Object enableProvider(String addr, String mk) {
		log.warn("Enable provider {} for service {}", addr, mk);
		rpcContext.enableProvider(addr, this.getKey(mk));
		return true;
	}
	
	protected String getKey(String mk){
		String[] a = mk.split("/");
		return String.format("%s/%s/%s/%s", a[0], a[1], a[2], a[3]);
	}
}
