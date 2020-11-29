package com.boarsoft.rpc.http.jetty;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.boarsoft.rpc.core.RpcContext;
import com.boarsoft.rpc.http.jetty.connector.ConnectorBuilder;
import com.boarsoft.rpc.http.jetty.servlet.GatewayServlet;

public class JettyGateway {
	private static final Logger log = LoggerFactory.getLogger(JettyGateway.class);

	@Autowired
	protected RpcContext rpcContext;

	/** 内嵌Jetty服务器 */
	protected Server server = new Server();

	protected ServletHandler handler = new ServletHandler();

	protected ProxyServlet servlet = new GatewayServlet();

	protected Map<String, String> params = new HashMap<String, String>();

	protected Set<ConnectorBuilder> connectorBuilders;

	@PostConstruct
	public void init() throws Exception {
		server.setHandler(handler);
		ServletHolder sh = new ServletHolder(servlet);
		sh.setAsyncSupported(true);
		if (params.isEmpty() || !params.containsKey("maxThreads")) {
			params.put("maxThreads", "100");
		}
		sh.setInitParameters(params);
		handler.addServletWithMapping(sh, "/*");

		// TODO 通过过滤器链提供扩展
		// FilterHolder fh = handler.addFilterWithMapping(GatewayFilter.class,
		// "/*",
		// EnumSet.of(DispatcherType.REQUEST));
		// // fh.setInitParameter("initParamKey", "InitParamValue");
		// context.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));
		// context.addServlet(sh, "/*");

		// 提供HTTP1/HTTP2连接器
		for (ConnectorBuilder cb : connectorBuilders) {
			server.addConnector(cb.build(server));
		}
		server.start();
		log.info("Jetty gateway server started");
	}

	public Set<ConnectorBuilder> getConnectorBuilders() {
		return connectorBuilders;
	}

	public void setConnectorBuilders(Set<ConnectorBuilder> connectorBuilders) {
		this.connectorBuilders = connectorBuilders;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public ServletHandler getHandler() {
		return handler;
	}

	public void setHandler(ServletHandler handler) {
		this.handler = handler;
	}

	public ProxyServlet getServlet() {
		return servlet;
	}

	public void setServlet(ProxyServlet servlet) {
		this.servlet = servlet;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
}
