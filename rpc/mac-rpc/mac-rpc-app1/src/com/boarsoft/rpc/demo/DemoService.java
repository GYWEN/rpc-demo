package com.boarsoft.rpc.demo;

import com.boarsoft.rpc.bean.RpcMethod;

public interface DemoService {
	/**
	 * 同步方法调用
	 * 
	 * @param u
	 * @return
	 */
	String helloSC(User u);

	/**
	 * 异步调用方法
	 * 
	 * @param u
	 * @return 消费方调用时立即返回Future对象，服务方实现时则需要返回真实对象
	 */
	Object helloAC(User u);

	Object helloSB(User u);

	Object helloAB(User u);

	Object helloAN(User u); // 异步通知

	Object helloSN(User u); // 同步通知

	Object helloBN(User u); // 广播通知

	String hello(User u, long w, int s);

	String hello(User[] ua, long[] w, Integer[][] s, String[] a);

	String hello(User u);

	@RpcMethod(id = 99)
	String hello1(User u);
}
