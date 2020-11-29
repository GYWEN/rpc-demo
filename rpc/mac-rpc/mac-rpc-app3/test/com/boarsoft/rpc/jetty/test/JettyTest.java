package com.boarsoft.rpc.jetty.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.boarsoft.common.util.HttpClientUtil;
import com.boarsoft.common.util.JsonUtil;
import com.boarsoft.rpc.demo.User;

public class JettyTest {

	@Test
	public void test1() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		// headers.put("service",
		// "demo/demo1/com.boarsoft.rpc.demo.DemoService/1.0");
		// headers.put("method", "hello(com.boarsoft.rpc.demo.User)");

		User u = new User("Mac_J");
		String body = new StringBuilder()//
				.append("com.boarsoft.rpc.demo.User=")//
				.append(JsonUtil.toJSONString(u))//
				.toString();

		String url = "http://localhost:8803/demo/hello2.do";
		String rs = HttpClientUtil.sendPost(url, "application/json", headers, body);
		System.out.println(rs);
	}

	@Test
	public void test2() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		// headers.put("service",
		// "demo/demo1/com.boarsoft.rpc.demo.DemoService/1.0");
		// headers.put("method", "hello(com.boarsoft.rpc.demo.User)");

		User u = new User("Mac_J");
		Map<String, String> params = new HashMap<String, String>();
		params.put("user", JSON.toJSONString(u));

		String url = "http://localhost:8803/demo/hello2";
		String rs = HttpClientUtil.sendPost(url, "application/x-www-form-urlencoded", headers, params);
		System.out.println(rs);
	}

	@Test
	public void test3() throws IOException {
		Map<String, String> headers = new HashMap<String, String>();
		// headers.put("service",
		// "demo/demo1/com.boarsoft.rpc.demo.DemoService/1.0");
		// headers.put("method", "hello(com.boarsoft.rpc.demo.User)");

		User u = new User("Mac_J");
		String body = new StringBuilder()//
				.append("com.boarsoft.rpc.demo.User=")//
				.append(JsonUtil.toJSONString(u))//
				.toString();

		String url = "https://localhost:8443/demo/hello2.do";
		String rs = HttpClientUtil.sendPost(url, "application/json", headers, body);
		System.out.println(rs);
	}

}
