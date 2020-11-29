package com.boarsoft.rpc.http.tomcat.handler;

import java.io.IOException;
import java.lang.reflect.Parameter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.http.mapper.ParamMapper;

/**
 * 将客户端/前端提交的表单字段名与方法参数进行匹配，并根据方法参数类型自动转换<br>
 * 允许参数的缺失，留给应用自行扩展补充
 * 
 * @author Mac_J
 *
 */
public class FormalJsonIoHandler extends AbstractJsonIoHandler {

	protected ParamMapper paramMapper;

	@Override
	public RpcCall read(RpcMethodConfig mc, RpcCall co, String target, HttpServletRequest req, HttpServletResponse rsp)
			throws IOException, ClassNotFoundException {
		Parameter[] params = mc.getParameters();
		Object[] args = RpcCall.EMPTY_ARGS;
		if (params.length > 0) {
			args = new Object[params.length];
			for (int i = 0; i < params.length; i++) {
				Parameter p = params[i];
				// 需要 java8 的 javac -parameters
				String k = p.getName();
				String v = req.getParameter(k);
				if ("null".equals(v)) {
					args[i] = null;
					continue;
				}
				paramMapper.map(args, i, p.getType(), k, v, req, rsp);
			}
		}
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
