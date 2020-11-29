package com.boarsoft.rpc.filter;

import java.util.concurrent.atomic.AtomicBoolean;

import com.boarsoft.rpc.bean.RpcMethodConfig;

public interface RpcFilter {
	/** 全局过滤器监控开关，默认打开 */
	AtomicBoolean status = new AtomicBoolean(true);

	boolean filter(RpcMethodConfig mc, Object[] args);

	boolean getStatus();

	void setStatus(boolean status);
}
