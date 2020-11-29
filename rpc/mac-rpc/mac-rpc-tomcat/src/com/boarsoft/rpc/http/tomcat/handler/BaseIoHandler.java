package com.boarsoft.rpc.http.tomcat.handler;

import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boarsoft.bean.ReplyInfo;
import com.boarsoft.common.util.JsonUtil;
import com.boarsoft.common.util.StreamUtil;

public abstract class BaseIoHandler implements IoHandler {
	/** */
	protected String charset = "UTF-8";
	/** */
	protected String contentType = "application/json; charset=UTF-8";

	public String getCookie(HttpServletRequest request, String key) {
		Cookie[] ca = request.getCookies();
		if ((ca == null) || (ca.length < 1)) {
			return null;
		}
		Cookie[] arrayOfCookie1;
		int j = (arrayOfCookie1 = ca).length;
		for (int i = 0; i < j; i++) {
			Cookie c = arrayOfCookie1[i];
			if (c.getName().equals(key)) {
				return c.getValue();
			}
		}
		return null;
	}

	public void write(HttpServletResponse response, String str) {
		if (str == null) {
			str = "";
		}
		response.setContentType("text/html;charset=UTF-8");
		try {
			response.getWriter().write(str);
			response.flushBuffer();
		} catch (IOException e) {
			try {
				StreamUtil.close(response.getOutputStream());
			} catch (IOException e1) {
			}
		}
	}

	public void writeXml(HttpServletResponse response, String xml) {
		xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xml;
		write(response, xml.replaceAll("\n", "").replaceAll("\r", ""));
	}

	public void writeReply(HttpServletResponse response) {
		write(response, "{ \"success\": true }");
	}

	public void writeReply(HttpServletResponse response, String data) {
		write(response, "{ \"success\": false, \"data\": ".concat(data).concat(" }"));
	}

	public void writeTextReply(HttpServletResponse response, String data) {
		write(response, "{ \"success\": false, \"data\": \"".concat(data).concat("\" }"));
	}

	public void writeTextReply(HttpServletResponse response, boolean b, String data) {
		StringBuffer sb = new StringBuffer();
		sb.append("{ \"success\": ").append(b).append(", \"data\": \"");
		sb.append(data).append("\" }");
		write(response, sb.toString());
	}

	public void writeReply(HttpServletResponse response, boolean b, String data) {
		StringBuffer sb = new StringBuffer();
		sb.append("{ \"success\": ").append(b).append(", \"data\": ");
		sb.append(data).append(" }");
		write(response, sb.toString());
	}

	public void writeReply(HttpServletResponse response, String msg, String params) {
		writeReply(response, msg, params.split(","));
	}

	public void writeReply(HttpServletResponse response, boolean b, String msg, String params) {
		writeReply(response, b, msg, params.split(","));
	}

	public void writeReply(HttpServletResponse response, String msg, String[] params) {
		String ps = params == null ? "[]" : JsonUtil.from(params);
		StringBuffer sb = new StringBuffer();
		sb.append("{ \"success\": false, \"data\": ").append(msg);
		sb.append(", \"params\": ").append(ps).append(" }");
		write(response, sb.toString());
	}

	public void writeReply(HttpServletResponse response, boolean b, String msg, String[] params) {
		String ps = params == null ? "[]" : JsonUtil.from(params);
		StringBuffer sb = new StringBuffer();
		sb.append("{ \"success\": ").append(b).append(", \"data\": ").append(msg);
		sb.append(", \"params\": ").append(ps).append(" }");
		write(response, sb.toString());
	}

	public void writeReply(HttpServletResponse response, ReplyInfo<String> o) {
		StringBuilder sb = new StringBuilder();
		if (o.getData() != null) {
			sb.append("\"").append(o.getData()).append("\"");
		}
		writeReply(response, o.isSuccess(), sb.toString(), o.getParams());
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}
}
