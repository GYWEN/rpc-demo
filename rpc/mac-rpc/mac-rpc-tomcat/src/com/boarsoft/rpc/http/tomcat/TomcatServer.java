package com.boarsoft.rpc.http.tomcat;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.boarsoft.common.Util;
import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcContext;
import com.boarsoft.rpc.http.tomcat.handler.RpcTomcatServlet;

/**
 * 基于Tomcat的服务器包装类<br>
 * 将当前节点的每个RPC服务方法暴露为对应的HTTP服务
 * 
 * @author Mac_J
 *
 */
public class TomcatServer {
	private static final Logger log = LoggerFactory.getLogger(TomcatServer.class);

	@Autowired
	protected RpcContext rpcContext;

	/** */
	protected RpcTomcatServlet rpcTomcatServlet;
	/** */
	protected Tomcat tomcat = new Tomcat();

	/** */
	protected String hostname = "localhost";
	/** */
	protected int port = 8080;
	/** */
	protected String contextPath = "";
	/** */
	protected String suffix = ".do";

	protected List<Connector> connectorList;

	@PostConstruct
	public void init() throws Exception {
		// tomcat.setPort(port);
		tomcat.setHostname(hostname);
		// 创建上下文
		StandardContext context = new StandardContext();
		context.setPath(contextPath);
		context.addLifecycleListener(new Tomcat.FixContextListener());

		Host host = tomcat.getHost();
		host.setAutoDeploy(false);
		host.addChild(context);

		tomcat.addServlet(contextPath, "rpcTomcatServlet", rpcTomcatServlet);

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
				context.addServletMappingDecoded(uri, "rpcTomcatServlet");
			}
		}

		for (Connector connector : connectorList) {
			tomcat.getService().addConnector(connector);
		}
		tomcat.start();
		log.info("Tomcat embed start at {} successfully.", port);
	}

	@PreDestroy
	public void stop() {
		if (this.tomcat == null) {
			return;
		}
		try {
			this.tomcat.stop();
			this.tomcat = null;
		} catch (Exception e) {
			log.error("Error on stop jetty server", e);
		}
	}

	public RpcContext getRpcContext() {
		return rpcContext;
	}

	public void setRpcContext(RpcContext rpcContext) {
		this.rpcContext = rpcContext;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public Tomcat getTomcat() {
		return tomcat;
	}

	public void setTomcat(Tomcat tomcat) {
		this.tomcat = tomcat;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public RpcTomcatServlet getRpcTomcatServlet() {
		return rpcTomcatServlet;
	}

	public void setRpcTomcatServlet(RpcTomcatServlet rpcTomcatServlet) {
		this.rpcTomcatServlet = rpcTomcatServlet;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public List<Connector> getConnectorList() {
		return connectorList;
	}

	public void setConnectorList(List<Connector> connectorList) {
		this.connectorList = connectorList;
	}

}
