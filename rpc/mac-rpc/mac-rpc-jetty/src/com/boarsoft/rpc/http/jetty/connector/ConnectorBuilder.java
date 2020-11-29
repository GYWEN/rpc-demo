package com.boarsoft.rpc.http.jetty.connector;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

public interface ConnectorBuilder {
	/**
	 * 
	 * @param server
	 * @return
	 */
	Connector build(Server server);
}
