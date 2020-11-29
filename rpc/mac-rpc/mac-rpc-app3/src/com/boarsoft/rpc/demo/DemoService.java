package com.boarsoft.rpc.demo;

import com.boarsoft.rpc.bean.RpcMethod;
import com.boarsoft.rpc.bean.RpcUri;

public interface DemoService {
	/**
	 * 同步方法调用
	 * 
	 * @param u
	 * @return
	 */
	String helloSC(User user);

	/**
	 * 异步调用方法
	 * 
	 * @param u
	 * @return 消费方调用时立即返回Future对象，服务方实现时则需要返回真实对象
	 */
	Object helloAC(User user);

	Object helloSB(User user);

	Object helloAB(User user);

	Object helloAN(User user); // 异步通知

	Object helloSN(User user); // 同步通知

	Object helloBN(User user); // 广播通知

	@RpcUri("/hello3")
	String hello(User user, long w, int s);

	@RpcUri("/hello4")
	String hello(User[] users, long[] w, Integer[][] s, String[] a);

	String hello(User user);

	@RpcMethod(id = 99)
	String hello1(User user);
}
