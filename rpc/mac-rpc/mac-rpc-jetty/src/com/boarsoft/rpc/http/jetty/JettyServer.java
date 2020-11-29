package com.boarsoft.rpc.http.jetty;

import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcContext;
import com.boarsoft.rpc.http.jetty.connector.ConnectorBuilder;
import com.boarsoft.rpc.http.jetty.handler.RpcJettyHandler;

/**
 * 基于Jetty HTTP_1.1/HTTP_2 服务器包装类<br>
 * 将当前节点的每个RPC服务方法暴露为对应的HTTP服务
 * 
 * @author Mac_J
 *
 */
public class JettyServer {
	private static final Logger log = LoggerFactory.getLogger(JettyServer.class);

	/** 内嵌Jetty服务器 */
	protected Server server = new Server();

	@Autowired
	protected RpcContext rpcContext;

	protected RpcJettyHandler handler;

	protected Set<ConnectorBuilder> connectorBuilders;

	protected String suffix = ".do";

	@PostConstruct
	public void init() throws Exception {
		StringBuilder sb = new StringBuilder();
		Map<String, RpcServiceConfig> scMap = rpcContext.getLocalServiceConfigMap();
		for (RpcServiceConfig sc : scMap.values()) {
			if (Util.strIsEmpty(sc.getUri())) {
				continue;
			}
			Map<String, RpcMethodConfig> mcMap = sc.getMethodConfigMap();
			for (RpcMethodConfig mc : mcMap.values()) {
				if (Util.strIsEmpty(mc.getUri())) {
					continue;
				}
				sb.setLength(0);
				String uri = sb.append(sc.getUri()).append(mc.getUri())//
						.append(suffix).toString();
				log.info("{ uri: '{}', service: '{}', method: '{}' }", //
						uri, sc.getSign(), mc.getSign());
				handler.map(uri, mc);
			}
		}
		ContextHandler ch = new ContextHandler("/");
		ch.setHandler(handler);
		server.setHandler(ch);
		for (ConnectorBuilder cb : connectorBuilders) {
			server.addConnector(cb.build(server));
		}
		server.start();
	}

	@PreDestroy
	public void stop() {
		if (this.server == null) {
			return;
		}
		try {
			this.server.stop();
			this.server = null;
		} catch (Exception e) {
			log.error("Error on stop jetty server", e);
		}
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public RpcJettyHandler getHandler() {
		return handler;
	}

	public void setHandler(RpcJettyHandler handler) {
		this.handler = handler;
	}

	public Set<ConnectorBuilder> getConnectorBuilders() {
		return connectorBuilders;
	}

	public void setConnectorBuilders(Set<ConnectorBuilder> connectorBuilders) {
		this.connectorBuilders = connectorBuilders;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
}
