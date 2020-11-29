package com.boarsoft.common.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class ReflectUtil {

	public static final Map<String, Class<?>> typeMap = new HashMap<String, Class<?>>();

	static {
		typeMap.put("byte", byte.class);
		typeMap.put("boolean", boolean.class);
		typeMap.put("short", short.class);
		typeMap.put("int", int.class);
		typeMap.put("long", long.class);
		typeMap.put("float", float.class);
		typeMap.put("double", double.class);
	}

	/**
	 * 返回方法紧凑格式的签名
	 * 
	 * @return
	 */
	public static String getMethodSign(Method method) {
		StringBuilder sb = new StringBuilder();
		sb.append(method.getName()).append("(");
		Class<?>[] pca = method.getParameterTypes();
		if (pca.length > 0) {
			for (Class<?> c : pca) {
				sb.append(c.getName()).append(",");
			}
			sb.setLength(sb.length() - 1);
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * 解析方法的紧凑格式
	 * 
	 * @param clazz
	 * @param sign
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 */
	public static Method getMethod(Class<?> clazz, String sign)
			throws NoSuchMethodException, SecurityException, ClassNotFoundException {
		int left = sign.indexOf("(");
		// 取方法名
		String name = sign.substring(left);
		// 取参数类型
		String ps = sign.substring(left + 1, sign.length() - 1);
		String[] pa = ps.split(",");
		Class<?>[] pca = new Class<?>[pa.length];
		for (int i = 0; i < pa.length; i++) {
			String c = pa[i];
			pca[i] = Class.forName(c);
		}
		Method m = clazz.getMethod(name, pca);
		if (m == null) {
			m = clazz.getDeclaredMethod(name, pca);
		}
		return m;
	}

	/**
	 * 解析 method.toString() 这种完整格式
	 * 
	 * @param sign
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws ClassNotFoundException
	 */
	public static Method getMethod(String sign) throws NoSuchMethodException, SecurityException, ClassNotFoundException {
		int left = sign.indexOf("(");
		// 取类名
		String name = sign.substring(0, left);
		int lastBlank = name.lastIndexOf(" ");
		int lastDot = name.lastIndexOf(".");
		String className = name.substring(lastBlank + 1, lastDot);
		Class<?> clazz = Class.forName(className);
		// 取方法名
		name = name.substring(lastDot + 1);
		// 取参数类型
		int right = sign.lastIndexOf(")");
		String ps = sign.substring(left + 1, right);
		String[] pa = ps.split(",");
		Class<?>[] pca = new Class<?>[pa.length];
		for (int i = 0; i < pa.length; i++) {
			String c = pa[i];
			if (typeMap.containsKey(c)) {
				pca[i] = typeMap.get(c);
			} else {
				pca[i] = Class.forName(c);
			}
		}
		Method m = clazz.getMethod(name, pca);
		if (m == null) {
			m = clazz.getDeclaredMethod(name, pca);
		}
		return m;
	}
}
