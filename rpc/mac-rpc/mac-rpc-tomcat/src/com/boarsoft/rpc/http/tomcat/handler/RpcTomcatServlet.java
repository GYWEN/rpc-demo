package com.boarsoft.rpc.http.tomcat.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.bean.RpcStub;
import com.boarsoft.rpc.core.RpcContext;

public class RpcTomcatServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(RpcTomcatServlet.class);

	@Autowired
	protected RpcContext rpcContext;

	protected Map<String, IoHandler> ioHandlerMap;

	protected Map<String, RpcMethodConfig> uriMap = new HashMap<String, RpcMethodConfig>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
		// 只允许POST方法
		rsp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse rsp) throws ServletException, IOException {
		// 处理报文头
		String uri = req.getRequestURI();
		String remoteAddr = req.getRemoteAddr();
		String contentType = req.getContentType();
		String serviceKey = req.getHeader("service"); // 可选
		String methodKey = req.getHeader("method"); // 可选
		// 有传uri则根据uri取配置
		if (Util.strIsNotEmpty(uri)) {
			RpcMethodConfig mc = uriMap.get(uri);
			if (mc != null) {
				serviceKey = mc.getFaceConfig().getSign();
				methodKey = mc.getSign();
			}
		}
		if ((Util.strIsEmpty(serviceKey) || Util.strIsEmpty(methodKey))) {
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		//
		IoHandler ioHandler = ioHandlerMap.get(contentType);
		if (ioHandler == null) {
			rsp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
			return;
		}
		log.info("{} is invoking {}/{} via HTTP RPC protocol {}, handler is {}", //
				remoteAddr, serviceKey, methodKey, contentType, ioHandler);
		// 构建服务调用参数
		RpcServiceConfig sc = rpcContext.getLocalServiceConfigMap().get(serviceKey);
		RpcMethodConfig mc = sc.getMethodConfigMap().get(methodKey);
		int methodId = mc.getRelativeId();
		RpcStub ref = rpcContext.getStub(remoteAddr);
		RpcCall co = new RpcCall();
		co.setType(RpcCall.TYPE_REQUEST);
		co.setMethodId(mc.getRelativeId());
		co.setMethodExeNo(ref.getMethodExeNo(methodId));
		try {
			co = ioHandler.read(mc, co, null, req, rsp);
		} catch (Exception e) {
			log.error("Error on read request data", e);
			rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		// 调用服务
		try {
			rpcContext.invoke(co, mc, null, req.getRemoteAddr());
		} catch (Exception e) {
			log.error("Error on process request {}", co, e);
			co.setThrowable(e);
		} finally {
			co.setType(RpcCall.TYPE_RESPONSE);
			co.setArguments(RpcCall.EMPTY_ARGS);
			try {
				ioHandler.write(mc, co, null, req, rsp);
			} catch (Exception e) {
				log.error("Error on write response {}", co, e);
			}
		}
	}

	public void map(String uri, RpcMethodConfig mc) {
		uriMap.put(uri, mc);
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public Map<String, IoHandler> getIoHandlerMap() {
		return ioHandlerMap;
	}

	public void setIoHandlerMap(Map<String, IoHandler> ioHandlerMap) {
		this.ioHandlerMap = ioHandlerMap;
	}

}