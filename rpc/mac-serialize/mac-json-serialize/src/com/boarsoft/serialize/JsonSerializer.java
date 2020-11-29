package com.boarsoft.serialize;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.alibaba.fastjson.JSON;

public class JsonSerializer implements ObjectSerializer {
	protected String charset;

	@Override
	public byte[] serialize(Object obj) throws IOException {
		return JSON.toJSONString(obj).getBytes(charset);
	}

	@Override
	public void serialize(Object obj, OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(this.serialize(obj));
		oos.flush();
	}

	public <T> T deserialize(ByteBuffer buf, Class<T> clazz) throws IOException, ClassNotFoundException {
		return deserialize(buf.array(), clazz);
	}

	@Override
	public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException {
		return deserialize(new ByteArrayInputStream(bytes), clazz);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(InputStream is, Class<T> clazz) throws IOException, ClassNotFoundException {
		return (T) this.deserialize(is);
	}

	@Override
	public Object deserialize(InputStream is) throws IOException, ClassNotFoundException {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String ln = null;
		StringBuilder sb = new StringBuilder();
		while ((ln = br.readLine()) != null) {
			sb.append(ln);
		}
		return sb.toString();
	}

	@Override
	public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		return new String(bytes, charset);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] b, T co) throws ClassNotFoundException, IOException {
		return (T) this.deserialize(b, co.getClass());
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}
}
