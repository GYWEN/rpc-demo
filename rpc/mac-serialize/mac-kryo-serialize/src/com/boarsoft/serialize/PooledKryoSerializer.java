package com.boarsoft.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;

public class PooledKryoSerializer implements ObjectSerializer, KryoFactory {
	// Build pool with SoftReferences enabled (optional)
	protected KryoPool pool = new KryoPool.Builder(this).softReferences().build();

	public Kryo create() {
		Kryo kryo = new Kryo();
		// // 默认为true，支持对象引用与循环引用情况
		// kryo.setReferences(true);
		// // 默认为false，表示无需预先注册类
		// kryo.setRegistrationRequired(false);
		// configure kryo instance, customize settings
		// copy from dubbo
		kryo.register(GregorianCalendar.class);
		kryo.register(InvocationHandler.class);
		kryo.register(BigDecimal.class);
		kryo.register(BigInteger.class);
		kryo.register(Pattern.class);
		kryo.register(BitSet.class);
		kryo.register(URI.class);
		kryo.register(UUID.class);
		kryo.register(HashMap.class);
		kryo.register(ArrayList.class);
		kryo.register(LinkedList.class);
		kryo.register(HashSet.class);
		kryo.register(TreeSet.class);
		kryo.register(Hashtable.class);
		kryo.register(Date.class);
		kryo.register(Calendar.class);
		kryo.register(ConcurrentHashMap.class);
		kryo.register(SimpleDateFormat.class);
		kryo.register(Vector.class);
		kryo.register(BitSet.class);
		kryo.register(StringBuffer.class);
		kryo.register(StringBuilder.class);
		kryo.register(Object.class);
		kryo.register(Object[].class);
		kryo.register(String[].class);
		kryo.register(byte[].class);
		kryo.register(char[].class);
		kryo.register(int[].class);
		kryo.register(float[].class);
		kryo.register(double[].class);
		// added
		kryo.register(Throwable.class);
		kryo.register(Exception.class);
		// added
		return kryo;
	}

	protected void writeClassAndObject(final Output output, final Object object) {
		// pool.run(new KryoCallback<Object>() {
		// public Object execute(Kryo kryo) {
		// kryo.writeClassAndObject(output, object);
		// return null;
		// }
		// });
		Kryo kryo = pool.borrow();
		try {
			kryo.writeClassAndObject(output, object);
		} finally {
			pool.release(kryo);
		}
	}

	protected Object readClassAndObject(final Input input) {
		// return pool.run(new KryoCallback<Object>() {
		// public Object execute(Kryo kryo) {
		// return kryo.readClassAndObject(input);
		// }
		// });
		Kryo kryo = pool.borrow();
		try {
			return kryo.readClassAndObject(input);
		} finally {
			pool.release(kryo);
		}
	}

	@Override
	public byte[] serialize(Object obj) throws IOException {
		Output output = new Output(4096, -1);
		try {
			this.writeClassAndObject(output, obj);
			output.flush();
			return output.toBytes();
		} finally {
			output.close();
		}
	}

	@Override
	public void serialize(Object obj, OutputStream os) throws IOException {
		Output output = new Output(os);
		this.writeClassAndObject(output, obj);
		output.flush();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
		Input input = new Input(bytes);
		return (T) this.readClassAndObject(input);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(InputStream is, Class<T> clazz) throws IOException {
		Input input = new Input(is);
		return (T) this.readClassAndObject(input);
	}

	@Override
	public Object deserialize(byte[] bytes) throws IOException {
		Input input = new Input(bytes);
		return this.readClassAndObject(input);
	}

	@Override
	public Object deserialize(InputStream is) throws IOException {
		Input input = new Input(is);
		return this.readClassAndObject(input);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, T o) throws ClassNotFoundException, IOException {
		return (T) deserialize(bytes, o.getClass());
	}
}
