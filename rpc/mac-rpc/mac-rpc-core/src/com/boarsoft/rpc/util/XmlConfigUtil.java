package com.boarsoft.rpc.util;

import org.dom4j.Node;
import org.springframework.util.StringUtils;

public class XmlConfigUtil {
	public static String getStringAttr(Node rn, String xpath, String value) {
		Node n = rn.selectSingleNode(xpath);
		if (n != null) {
			return n.getStringValue();
		}
		return value;
	}

	public static Integer getIntegerAttr(Node rn, String xpath, Integer value) {
		String v = getStringAttr(rn, xpath, null);
		if (StringUtils.isEmpty(v)) {
			return value;
		}
		return Integer.valueOf(Integer.parseInt(v));
	}

	public static boolean getBooleanAttr(Node rn, String xpath, boolean value) {
		String v = getStringAttr(rn, xpath, null);
		if (StringUtils.isEmpty(v)) {
			return value;
		}
		return Boolean.parseBoolean(v);
	}
}
