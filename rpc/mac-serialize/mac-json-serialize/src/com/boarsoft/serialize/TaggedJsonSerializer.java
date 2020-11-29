package com.boarsoft.serialize;

import java.io.IOException;

import com.alibaba.fastjson.JSON;

public class TaggedJsonSerializer {

	public static String serialize(Object obj) throws IOException {
		if (obj == null) {
			return "null=null";
		}
		return new StringBuilder().append(obj.getClass().getName())//
				.append("=").append(JSON.toJSONString(obj)).toString();
	}

	public static String serialize(Object[] args) {
		if (args == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for (Object a : args) {
			if (a == null) {
				sb.append("null=null\n");
			} else {
				sb.append(a.getClass().getName()).append("=")//
						.append(JSON.toJSONString(a)).append("\n");
			}
		}
		if (sb.length() > 1) {
			sb.setLength(sb.length() - 1);
		}
		return sb.toString();
	}

	public static Object[] deserialize(String str) throws ClassNotFoundException {
		if (str == null || "".equals(str)) {
			return new Object[0];
		}
		String[] arr = str.split("\n");
		Object[] args = new Object[arr.length];
		for (int i = 0; i < arr.length; i++) {
			args[i] = deserialize1(arr[i]);
		}
		return args;
	}

	public static Object deserialize1(String ln) throws ClassNotFoundException {
		int i = ln.indexOf("=");
		String className = ln.substring(0, i);
		if ("null".equals(className) || "".equals(className)) {
			return null;
		}
		Class<?> clazz = Class.forName(className);
		String json = ln.substring(i + 1);
		return JSON.parseObject(json, clazz);
	}

	public static <T> T deserialize1(String ln, Class<T> clazz) throws ClassNotFoundException {
		int i = ln.indexOf("=");
		String className = ln.substring(0, i);
		if ("null".equals(className) || "".equals(className)) {
			return null;
		}
		String json = ln.substring(i + 1);
		return JSON.parseObject(json, clazz);
	}
}
