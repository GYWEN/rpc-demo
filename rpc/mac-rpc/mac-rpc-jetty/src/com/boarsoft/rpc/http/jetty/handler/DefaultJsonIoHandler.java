package com.boarsoft.rpc.http.jetty.handler;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boarsoft.common.Util;
import com.boarsoft.common.util.HttpClientUtil;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.http.mapper.ParamMapper;

/**
 * 解析客户端/前端以XML POST形式提交的字符串，此字符串必须依序包含所有的参数值，形如：<br>
 * xxx.xxx.Xxx={...}
 * 
 * @author Mac_J
 *
 */
@Deprecated
public class DefaultJsonIoHandler extends AbstractJsonIoHandler {

	protected ParamMapper paramMapper;

	@Override
	public RpcCall read(RpcMethodConfig mc, RpcCall co, String target, HttpServletRequest req, HttpServletResponse rsp)
			throws IOException, ClassNotFoundException {
		String body = HttpClientUtil.readStr(req.getInputStream(), charset);
		if (Util.strIsEmpty(body)) {
			co.setArguments(RpcCall.EMPTY_ARGS);
			return co;
		}
		//
		String[] arr = body.split("\n");
		Object[] args = new Object[arr.length];
		for (int i = 0; i < arr.length; i++) {
			String ln = arr[i];
			int j = ln.indexOf("=");
			String className = ln.substring(0, j);
			String json = ln.substring(j + 1);
			if ("null".equals(json)) {
				args[i] = null;
				continue;
			}
			Class<?> clazz = Class.forName(className);
			paramMapper.map(args, i, clazz, null, json, req, rsp);
		}
		// TODO 检查参数
		co.setArguments(args);
		return co;
	}

	public ParamMapper getParamMapper() {
		return paramMapper;
	}

	public void setParamMapper(ParamMapper paramMapper) {
		this.paramMapper = paramMapper;
	}

}
