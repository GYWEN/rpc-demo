package com.boarsoft.rpc.spy;

import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.rpc.bean.RpcMethodConfig;

public class RpcSvcInput {
	protected RpcCall call;
	protected RpcMethodConfig methodConfig;
	protected String remoteHost;

	public RpcSvcInput(RpcCall call, RpcMethodConfig methodConfig, String remoteHost) {
		this.call = call;
		this.methodConfig = methodConfig;
		this.remoteHost = remoteHost;
	}

	public RpcCall getCall() {
		return call;
	}

	public void setCall(RpcCall call) {
		this.call = call;
	}

	public RpcMethodConfig getMethodConfig() {
		return methodConfig;
	}

	public void setMethodConfig(RpcMethodConfig methodConfig) {
		this.methodConfig = methodConfig;
	}

	public String getRemoteHost() {
		return remoteHost;
	}

	public void setRemoteHost(String remoteHost) {
		this.remoteHost = remoteHost;
	}
}
