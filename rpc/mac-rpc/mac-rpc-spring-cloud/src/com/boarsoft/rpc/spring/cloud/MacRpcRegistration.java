package com.boarsoft.rpc.spring.cloud;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.client.serviceregistry.Registration;

import com.boarsoft.common.bean.InetConfig;
import com.boarsoft.rpc.RpcConfig;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcCore;

public class MacRpcRegistration implements Registration {
	protected RpcServiceConfig serviceConfig;

	public MacRpcRegistration(RpcServiceConfig serviceConfig) {
		this.serviceConfig = serviceConfig;
	}

	@Override
	public String getServiceId() {
		return new StringBuilder().append(RpcCore.getTag())//
				.append("-").append(serviceConfig.getRef()).toString();
	}

	@Override
	public String getHost() {
		return InetConfig.LOCAL_IP;
	}

	@Override
	public int getPort() {
		return RpcConfig.getInt("rpc.http.port", 8080);
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public URI getUri() {
		StringBuilder sb = new StringBuilder().append("/")//
				.append(RpcCore.getTag()).append("/")//
				.append(serviceConfig.getRef());
		try {
			return new URI(sb.toString());
		} catch (URISyntaxException e) {
			return null;
		}
	}

	@Override
	public Map<String, String> getMetadata() {
		Map<String, String> meta = new HashMap<String, String>();
		meta.put("group", serviceConfig.getGroup());
		meta.put("name", serviceConfig.getName());
		meta.put("interface", serviceConfig.getInterfaceName());
		meta.put("version", serviceConfig.getVersion());
		return meta;
	}

}
