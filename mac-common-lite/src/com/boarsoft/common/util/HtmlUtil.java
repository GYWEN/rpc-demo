package com.boarsoft.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtil {
	public static String filterHtmlTag(String rs) {
		Pattern pattern = Pattern.compile("<a[^<>]*>");
		Matcher mat = pattern.matcher(rs);
		while (mat.find()) {
			rs = rs.replace(mat.group(), "");
		}
		rs = rs.replace("</a>", "");
		return rs;
	}

	public static String grep(String s, String regex) {
		Pattern p = Pattern.compile(regex);
		Matcher mc = p.matcher(s);
		if (mc.find()) {
			return mc.group();
		}
		return null;
	}
}