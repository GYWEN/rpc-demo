package com.boarsoft.rpc;

public interface RpcCallback {
	void callback(Object result, Object... args);
}
