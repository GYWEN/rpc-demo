package com.boarsoft.rpc.serialize;

import java.io.IOException;
import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.rpc.bean.RpcCall;
import com.boarsoft.serialize.JavaSerializer;
import com.boarsoft.serialize.ObjectSerializer;

public class RpcSerializer {
	private static final Logger log = LoggerFactory.getLogger(RpcSerializer.class);

	/**
	 * 为避免耦合，这里不指定使用何种序列化组件，改为第一次使用RpcSerializer时才实始化。<br>
	 * 请配置SPI扩展文件：/META-INF/services/com.boarsoft.serialize.ObjectSerializer
	 */
	public static ObjectSerializer serializer = new JavaSerializer();

	static {
		// 从 /META-INF/services/com.boarsoft.serialize.ObjectSerializer 文件装载所有实现类
		ServiceLoader<ObjectSerializer> sl = ServiceLoader.load(ObjectSerializer.class);
		// 遍历所有实现类（我们只需要配置一个），取出并使用第一个作为当前的序列化组件
		Iterator<ObjectSerializer> it = sl.iterator();
		if (it.hasNext()) {
			serializer = it.next();
			log.info("Loaded object serializer {}", serializer.getClass().getName());
		}
	}

	public static byte[] serialize(RpcCall co) throws IOException {
		return serializer.serialize(co);
	}

	public static RpcCall deserialize(byte[] b) throws ClassNotFoundException, IOException {
		return serializer.deserialize(b, RpcCall.class);
	}

	public static RpcCall deserialize(RpcCall co, byte[] b) throws ClassNotFoundException, IOException {
		return serializer.deserialize(b, co);
	}

	// 注意此方法是非静态的
	public void setSerializer(ObjectSerializer serializer) {
		RpcSerializer.serializer = serializer;
	}
}
