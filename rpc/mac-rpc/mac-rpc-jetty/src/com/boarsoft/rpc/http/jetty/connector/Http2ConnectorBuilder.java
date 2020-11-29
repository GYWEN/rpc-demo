package com.boarsoft.rpc.http.jetty.connector;

import java.net.URL;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import com.boarsoft.rpc.RpcConfig;

/**
 * HTTP 2 connector
 * 
 * @author Mac_J
 *
 */
public class Http2ConnectorBuilder implements ConnectorBuilder {
	/** */
	protected int port = 8443;
	/** */
	protected String host = "localhost";
	/** */
	protected long idleTimeout = 3000000;
	/** */
	protected HttpConfiguration config;
	/** */
	protected String ksPassword = "boar-rpc";
	/** */
	protected String kmPassword = "boar-rpc";

	@Override
	public Connector build(Server server) {
		SslContextFactory ctxFactory = this.getSslContextFactory();
		// 使默认方法为h2
		SslConnectionFactory sslCf = new SslConnectionFactory(ctxFactory, "alpn");
		// SslConnectionFactory sslConFactory = new
		// SslConnectionFactory(sslContextFactory,HttpVersion.HTTP_1_1.toString());
		SecureRequestCustomizer src = new SecureRequestCustomizer();
		src.setStsMaxAge(2000);
		src.setStsIncludeSubDomains(true);

		// HTTPS Configuration
		HttpConfiguration conf = new HttpConfiguration(config);
		conf.addCustomizer(src);
		// HTTP/2 Connection Factory
		HTTP2ServerConnectionFactory http2Cf = new HTTP2ServerConnectionFactory(config);
		http2Cf.setMaxConcurrentStreams(128);
		// ALPN
		ALPNServerConnectionFactory alpnCf = new ALPNServerConnectionFactory();
		// HTTP/2 Connector
		ServerConnector connector = new ServerConnector(server, sslCf, //
				alpnCf, http2Cf, new HttpConnectionFactory(config));

		connector.setHost(host);
		connector.setPort(port);
		connector.setIdleTimeout(idleTimeout);
		return connector;
	}

	protected SslContextFactory getSslContextFactory() {
		// SSL Context Factory for HTTPS and HTTP/2
		String sslKeystore = "/com/boarsoft/rpc/http/jetty/key/server.jks";
		String keystorePath = RpcConfig.getProperty("ssl.keystore.path", sslKeystore);
		URL keyURL = Http2ConnectorBuilder.class.getResource(keystorePath);
		SslContextFactory ctxFactory = new SslContextFactory();
		ctxFactory.setKeyStoreResource(Resource.newResource(keyURL));
		ctxFactory.setKeyStorePassword(ksPassword);
		ctxFactory.setKeyManagerPassword(kmPassword);
		ctxFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
		ctxFactory.setUseCipherSuitesOrder(true);
		return ctxFactory;
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

	public String getKsPassword() {
		return ksPassword;
	}

	public void setKsPassword(String ksPassword) {
		this.ksPassword = ksPassword;
	}

	public String getKmPassword() {
		return kmPassword;
	}

	public void setKmPassword(String kmPassword) {
		this.kmPassword = kmPassword;
	}
}
