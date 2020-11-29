package com.boarsoft.rpc.http.jetty.connector;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

/**
 * HTTP 1.1 connector
 * 
 * @author Mac_J
 *
 */
public class Http1ConnectorBuilder implements ConnectorBuilder {
	/** */
	protected int port = 8080;
	/** */
	protected String host = "localhost";
	/** */
	protected long idleTimeout = 3000000;
	/** */
	protected HttpConfiguration config;

	@Override
	public Connector build(Server server) {
		ServerConnector sc = new ServerConnector(server, new HttpConnectionFactory(config));
		sc.setPort(port);
		sc.setHost(host);
		sc.setIdleTimeout(idleTimeout);
		return sc;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public long getIdleTimeout() {
		return idleTimeout;
	}

	public void setIdleTimeout(long idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

	public HttpConfiguration getConfig() {
		return config;
	}

	public void setConfig(HttpConfiguration config) {
		this.config = config;
	}
}
