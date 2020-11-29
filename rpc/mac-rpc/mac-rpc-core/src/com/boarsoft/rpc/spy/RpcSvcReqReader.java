package com.boarsoft.rpc.spy;

import com.boarsoft.rpc.bean.RpcMethodConfig;
import com.boarsoft.soagov.svc.SvcReqReader;

public class RpcSvcReqReader implements SvcReqReader {

	@Override
	public Object pick(Object input, String key) {
		RpcSvcInput p = (RpcSvcInput) input;
		if (DM_ADDR.equals(key)) {
			String addr = p.getRemoteHost();
			return addr;
		} else if (DM_IP.equals(key)) {
			String addr = p.getRemoteHost();
			return addr.split(":")[0];
		}
		return null;
	}

	@Override
	public String getCode(Object input) {
		RpcSvcInput p = (RpcSvcInput) input;
		RpcMethodConfig mc = p.getMethodConfig();
		return mc.getKey();
	}

}
