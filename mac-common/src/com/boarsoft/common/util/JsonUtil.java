package com.boarsoft.common.util;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSON;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import net.sf.json.JsonConfig;
import net.sf.json.processors.DefaultValueProcessor;
import net.sf.json.util.CycleDetectionStrategy;

@SuppressWarnings("rawtypes")
public class JsonUtil {
	private static final Map<Class, String> excludes = new HashMap<Class, String>();
	// private static final Gson gson = new Gson();

	private static DefaultValueProcessor nullValuePro = new DefaultValueProcessor() {
		public Object getDefaultValue(Class type) {
			return null;
		}
	};

	private JsonUtil() {
	}

	private static JsonConfig prepare(String ex[]) {
		JsonConfig jc = new JsonConfig();
		jc.registerDefaultValueProcessor(Integer.class, nullValuePro);
		jc.registerDefaultValueProcessor(Long.class, nullValuePro);
		jc.registerDefaultValueProcessor(Float.class, nullValuePro);
		jc.registerDefaultValueProcessor(Double.class, nullValuePro);
		jc.registerDefaultValueProcessor(String.class, nullValuePro);
		jc.registerDefaultValueProcessor(Boolean.class, nullValuePro);
		jc.registerDefaultValueProcessor(Boolean.class, nullValuePro);
		jc.setCycleDetectionStrategy(CycleDetectionStrategy.LENIENT);
		jc.setExcludes(ex);
		return jc;
	}

	public static Object toBean(String json, Class c) {
		return JSONObject.toBean(JSONObject.fromObject(json), c);
	}

	public static String getExcludes(Class c) {
		if (excludes.containsKey(c))
			return excludes.get(c);
		boolean b = false;
		StringBuffer sb = new StringBuffer();
		for (Field f : c.getDeclaredFields()) {
			if (Collection.class.isAssignableFrom(f.getType())) {
				if (b)
					sb.append(",");
				sb.append(f.getName());
				b = true;
			}
		}
		String s = sb.toString();
		excludes.put(c, s);
		return s;
	}

	public static String from(Object o) {
		return JSONObject.fromObject(o, prepare(null)).toString();
	}

	// public static String toJson(Object o) {
	// return gson.toJson(o);
	// }

	/**
	 * alibaba JSON lib
	 * 
	 * @param o
	 * @return
	 */
	public static String toJSONString(Object o) {
		return JSON.toJSONString(o);
	}

	public static String from(Object[] o) {
		return JSONArray.fromObject(o, prepare(null)).toString();
	}

	public static String from(Object o, String[] ex) {
		return JSONObject.fromObject(o, prepare(ex)).toString();
	}

	public static String from(Object o, String ex) {
		return JSONObject.fromObject(o, prepare(ex.split(","))).toString();
	}

	public static String from(Object o, Class ex) {
		return from(o, getExcludes(ex));
	}

	public static String from(Collection o) {
		return JSONArray.fromObject(o, prepare(null)).toString();
	}

	public static String from(Collection o, String[] ex) {
		return JSONArray.fromObject(o, prepare(ex)).toString();
	}

	public static String from(Collection o, String ex) {
		// JSON.toJSONString(object, config, features);
		// gson.toJson(src);
		return JSONArray.fromObject(o, prepare(ex.split(","))).toString();
	}

	public static String from(Collection o, Class ex) {
		return from(o, getExcludes(ex));
	}

	/**
	 * 多数情况会返回 DynaBean，有时则是其它对象，所以只能返回Object
	 * 
	 * @param str
	 * @return
	 */
	public static Object fromString(String str) {
		return JSONSerializer.toJava(JSONSerializer.toJSON(str));
	}

	/**
	 * alibaba JSON lib
	 * 
	 * @param json
	 * @param clazz
	 * @return
	 */
	public static <T> T parseObject(String json, Class<T> clazz) {
		return JSON.parseObject(json, clazz);
	}

	// public static <T> T fromJson(String json, Class<T> clazz) {
	// return gson.fromJson(json, clazz);
	// }
}