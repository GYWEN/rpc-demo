package com.boarsoft.rpc.spy;

import java.lang.reflect.Method;

import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.soagov.spy.SvcSpy;

public interface RpcSvcSpy extends SvcSpy {
	/**
	 * 此方法用于在服务消费端发起调用时直接模拟远程调用返回结果
	 * 
	 * @param mocker
	 * @param key
	 * @param m
	 * @param args
	 * @return
	 * @throws Exception
	 */
	Object mock(Object mocker, String key, Method m, Object[] args) throws Exception;

	/**
	 * 此方法用于在服务提供者一方模拟实际的服务方法返回结果
	 * 
	 * @param co
	 * @param mc
	 * @return
	 */
	Object mock(RpcCall co, RpcMethodConfig mc);
}
