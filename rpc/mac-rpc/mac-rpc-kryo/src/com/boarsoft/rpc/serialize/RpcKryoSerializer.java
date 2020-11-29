package com.boarsoft.rpc.serialize;

import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.serialize.PooledKryoSerializer;
import com.esotericsoftware.kryo.Kryo;

public class RpcKryoSerializer extends PooledKryoSerializer {
	@Override
	public Kryo create() {
		Kryo kryo = super.create();
		kryo.register(RpcCall.class);
		return kryo;
	}
}
