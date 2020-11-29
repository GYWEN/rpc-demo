package com.boarsoft.rpc.demo;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("demoService")
public class DemoServiceImpl implements DemoService {
	private static final Logger log = LoggerFactory.getLogger(DemoServiceImpl.class);

	@Override
	public String hello(User u, long w, int s) {
		log.info("Hello {}", u.getName());
		if (w > 0) {
			try {
				Thread.sleep(w);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		StringBuilder sb = new StringBuilder(u.getName());
		for (int i = 0; i < s; i++) {
			sb.append(".");
		}
		return sb.toString();
	}

	@Override
	public String hello(User u) {
		log.info("Hello {}, {}", u.getName(), new Date().getTime());
		// 模拟时延
		try {
			Thread.sleep(100L);
		} catch (InterruptedException e) {
		}
		// 模拟异常
		if (u == null || StringUtils.isEmpty(u.getName())) {
			throw new IllegalArgumentException("Invalid parameter");
		}
		return "Hello ".concat(u.getName());
	}

	@Override
	public String helloSC(User u) {
		return this.hello(u);
	}

	@Override
	public String helloAC(User u) {
		return this.hello(u);
	}

	@Override
	public Object helloSB(User u) {
		return this.hello(u);
	}

	@Override
	public Object helloAB(User u) {
		return this.hello(u);
	}

	@Override
	public String hello(User[] ua, long[] w, Integer[][] s, String[] a) {
		return a[0];
	}

	@Override
	public String helloAN(User u) {
		return this.hello(u);
	}

	@Override
	public String helloSN(User u) {
		return this.hello(u);
	}

	@Override
	public String helloBN(User u) {
		return this.hello(u);
	}

	@Override
	public String hello1(User u) {
		return this.hello(u);
	}
}
