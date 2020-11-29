package com.boarsoft.rpc.demo;

import org.springframework.stereotype.Component;

import com.boarsoft.rpc.RpcCallback;

@Component("demoCallback")
public class DemoCallbackImpl implements RpcCallback {

	@Override
	public void callback(Object result, Object... args) {
		System.out.println("Do callback with parameters:");
		for (Object a : args) {
			System.out.println(a);
		}
		System.out.print("result = ");
		System.out.println(result);
	}
}
