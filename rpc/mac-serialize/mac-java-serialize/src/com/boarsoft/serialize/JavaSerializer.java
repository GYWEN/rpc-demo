package com.boarsoft.serialize;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class JavaSerializer implements ObjectSerializer {
	@Override
	public byte[] serialize(Object obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		try {
			oos.writeObject(obj);
			oos.flush();
			return baos.toByteArray();
		} finally {
			oos.close();
		}
	}

	@Override
	public void serialize(Object obj, OutputStream os) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(os);
		oos.writeObject(obj);
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
		ObjectInputStream ois = null;
		if (is instanceof ObjectInputStream) {
			ois = (ObjectInputStream) is;
		} else {
			ois = new ObjectInputStream(is);
		}
		try {
			return ois.readObject();
		} finally {
			ois.close();
		}
	}

	@Override
	public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		return deserialize(new ByteArrayInputStream(bytes));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, T o) throws ClassNotFoundException, IOException {
		return (T) deserialize(bytes, o.getClass());
	}
}
