package com.boarsoft.rpc.serialize;

import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.serialize.FastSerializer;

public class RpcFastSerializer extends FastSerializer {
	static {
		conf.registerClass(RpcCall.class);
	}
}
