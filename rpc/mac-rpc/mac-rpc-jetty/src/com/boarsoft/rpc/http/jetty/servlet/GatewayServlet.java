package com.boarsoft.rpc.http.jetty.servlet;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.jetty.proxy.AsyncProxyServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.bean.RpcRegistry;
import com.boarsoft.rpc.core.RpcContext;
import com.boarsoft.rpc.router.RpcRouter;

public class GatewayServlet extends AsyncProxyServlet {
	private static final Logger log = LoggerFactory.getLogger(GatewayServlet.class);

	private static final long serialVersionUID = -4634847690862338373L;

	@Autowired
	protected RpcContext rpcContext;
	
	protected RpcRouter<HttpServletRequest> router;

	protected int port = 8080;

	@Override
	protected String rewriteTarget(HttpServletRequest req) {
		String key = req.getHeader("service");
		String addr = router.getProvider(req);
		if (Util.strIsEmpty(addr)) {
			log.debug("Can not find provider of service {}", key);
			return null;
		}
		log.debug("Found provider {} of service {}", addr, key);
		String[] a = addr.split(":");
		RpcRegistry rr = rpcContext.getRegistry(addr);
		if (rr != null) {
			Properties props = rr.getMeta();
			if (props != null) {
				port = Util.str2int(props.getProperty("rpc.http.port"), port);
			}
		}
		String url = new StringBuilder("http://").append(a[0]).append(":")//
				.append(port).append("/").toString();
		log.info("Rewrite target url to {}", url);
		return url;
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public RpcRouter<HttpServletRequest> getRouter() {
		return router;
	}

	public void setRouter(RpcRouter<HttpServletRequest> router) {
		this.router = router;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
