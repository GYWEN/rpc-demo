package com.boarsoft.rpc.demo;

import org.springframework.stereotype.Component;

@Component("demoMocker")
public class DemoMocker implements DemoService {

	@Override
	public String helloSC(User u) {
		return "I'm a provider demo mocker";
	}

	@Override
	public Object helloAC(User u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object helloSB(User u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object helloAB(User u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String hello(User u, long w, int s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String hello(User[] ua, long[] w, Integer[][] s, String[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object helloAN(User u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object helloSN(User u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String hello(User u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object helloBN(User u) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String hello1(User u) {
		// TODO Auto-generated method stub
		return null;
	}

}
