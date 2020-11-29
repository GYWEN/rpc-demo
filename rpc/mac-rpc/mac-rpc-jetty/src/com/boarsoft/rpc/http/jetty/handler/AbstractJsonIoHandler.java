package com.boarsoft.rpc.http.jetty.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boarsoft.bean.ReplyInfo;
import com.boarsoft.common.util.JsonUtil;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;

public abstract class AbstractJsonIoHandler extends BaseIoHandler {

	@Override
	public void write(RpcMethodConfig mc, RpcCall co, String target, HttpServletRequest req, HttpServletResponse rsp)
			throws IOException {
		Object result = co.getResult();
		rsp.setContentType(contentType);
		// 如果服务方法返回的就是ReplyInfo，直接返回
		if (result != null && result instanceof ReplyInfo) {
			this.write(rsp, JsonUtil.toJSONString(result));
			return;
		}
		// 如果没有异常，返回成功
		Throwable e = co.getThrowable();
		if (e == null) {
			if (result != null) {
				rsp.setHeader("className", result.getClass().getName());
			}
			this.writeReply(rsp, true, JsonUtil.toJSONString(result));
			return;
		}
		// TODO 自定义异常处理
		this.writeReply(rsp, false, e.getMessage());
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
