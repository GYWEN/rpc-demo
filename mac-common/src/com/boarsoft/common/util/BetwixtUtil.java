package com.boarsoft.common.util;

import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.betwixt.io.BeanReader;
import org.apache.commons.betwixt.io.BeanWriter;

public class BetwixtUtil {
	private BetwixtUtil() {
	}

	@SuppressWarnings("rawtypes")
	public static Object xml2object(java.lang.Class c, String xml) {
		// 创建一个读取xml文件的流
		StringReader xmlReader = new StringReader(xml);
		// 创建一个BeanReader实例，相当于转换器
		BeanReader br = new BeanReader();
		// 配置BeanReader实例
		br.getXMLIntrospector().getConfiguration().setAttributesForPrimitives(false);
		br.getBindingConfiguration().setMapIDs(false); // 不自动生成ID
		try {
			// 注册要转换对象的类，并指定根节点名称
			br.registerBeanClass(c.getSimpleName(), c);
			// 将XML解析Java Object
			return br.parse(xmlReader);
		} catch (Exception e) {
			return null;
		}
	}

	public static String object2xml(Object o) {
		// 创建一个输出流，将用来输出Java转换的XML文件
		StringWriter sw = new StringWriter();
		// 输出XML的文件头
		// sw.append("<?xml version='1.0' ?>");
		// 创建一个BeanWriter实例，并将BeanWriter的输出重定向到指定的输出流
		BeanWriter bw = new BeanWriter(sw);
		// 配置BeanWriter对象
		bw.getXMLIntrospector().getConfiguration().setAttributesForPrimitives(false);
		bw.getBindingConfiguration().setMapIDs(false);
		// bw.enablePrettyPrint();
		try {
			bw.write(o);
			return sw.toString();
		} catch (Exception e) {
			return null;
		}
	}
}
