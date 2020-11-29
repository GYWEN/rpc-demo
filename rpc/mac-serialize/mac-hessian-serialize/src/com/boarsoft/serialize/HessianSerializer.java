package com.boarsoft.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

public class HessianSerializer implements ObjectSerializer {
	@Override
	public byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		HessianOutput ho = new HessianOutput(os);
		try {
			ho.writeObject(obj);
			ho.flush();
			return os.toByteArray();
		} finally {
			ho.close();
		}
	}

	@Override
	public void serialize(Object obj, OutputStream os) throws IOException {
		HessianOutput ho = new HessianOutput(os);
		ho.writeObject(obj);
		ho.flush();
	}

	@Override
	public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		return this.deserialize(is, clazz);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(InputStream is, Class<T> clazz) throws IOException {
		HessianInput hi = new HessianInput(is);
		return (T) hi.readObject();
	}

	@Override
	public Object deserialize(byte[] bytes) throws IOException {
		ByteArrayInputStream is = new ByteArrayInputStream(bytes);
		return this.deserialize(is);
	}

	@Override
	public Object deserialize(InputStream is) throws IOException {
		HessianInput hi = new HessianInput(is);
		return hi.readObject();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, T o) throws ClassNotFoundException, IOException {
		return (T) deserialize(bytes, o.getClass());
	}
}
