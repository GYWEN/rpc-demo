package com.boarsoft.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 对象序列化接口抽象
 * 
 * @author Mac_J
 *
 */
public interface ObjectSerializer {
	/**
	 * 将指定对象序列化为字节数组
	 * 
	 * @param obj
	 * @return
	 * @throws IOException
	 */
	byte[] serialize(Object obj) throws IOException;

	/**
	 * 将指定对象序列化并输出到流
	 * 
	 * @param obj
	 * @param os
	 * @throws IOException
	 */
	void serialize(Object obj, OutputStream os) throws IOException;

	/**
	 * 将字节数组反序列化为指定类型的对象（确定该对象会是何种类型时）
	 * 
	 * @param bytes
	 * @param clazz
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	<T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException;

	/**
	 * 从流中读取数据并反序化为指定类型的对象（确定该对象会是何种类型时）
	 * 
	 * @param is
	 * @param clazz
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	<T> T deserialize(InputStream is, Class<T> clazz) throws IOException, ClassNotFoundException;

	/**
	 * 从流中读取数据并反序化为对象（不确定对象类型时）
	 * 
	 * @param is
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	Object deserialize(InputStream is) throws IOException, ClassNotFoundException;

	/**
	 * 将字节数组反序列化为指定类型的对象（不确定对象类型时）
	 * 
	 * @param bytes
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException;

	/**
	 * 以对象T为模板，反序列化后得到新的T
	 * 
	 * @param b
	 * @param co
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	<T> T deserialize(byte[] b, T co) throws ClassNotFoundException, IOException;
}
