package com.boarsoft.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
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

/**
 * 通用Kryo序列化程序
 * 
 * @author Mac_J
 *
 */
public class KryoSerializer implements ObjectSerializer {
	// 使用ThreadLocal来为每个线程持有一个Kryo对象，使用软引用的目的是允许GC回收不用的Kryo对象
	protected ThreadLocal<SoftReference<Kryo>> threadLocal = new ThreadLocal<SoftReference<Kryo>>();

	protected Kryo getKryo() {
		SoftReference<Kryo> ref = threadLocal.get();
		Kryo kryo = null;
		if (ref == null || (kryo = ref.get()) == null) {
			kryo = new Kryo();
			// // 默认为true，支持对象引用与循环引用情况
			// kryo.setReferences(true);
			// // 默认为false，表示无需预先注册类
			// kryo.setRegistrationRequired(false);
			// 注册已知类
			this.register(kryo);
			threadLocal.set(new SoftReference<Kryo>(kryo));
		} else {
			kryo.reset();
		}
		return kryo;
	}

	protected void register(final Kryo kryo) {
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
	}

	@Override
	public byte[] serialize(Object obj) throws IOException {
		Output output = new Output(4096, -1);
		try {
			// this.getKryo().writeObject(output, obj);
			// 为了最大程序兼容（序列化时必须写入class信息，反序化时必须读取class信息
			this.getKryo().writeClassAndObject(output, obj);
			output.flush();
			return output.toBytes();
		} finally {
			output.close();
		}
	}

	@Override
	public void serialize(Object obj, OutputStream os) throws IOException {
		Output output = new Output(os);
		this.getKryo().writeClassAndObject(output, obj);
		output.flush();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
		Input input = new Input(bytes);
		// 为了最大程序兼容，序列化时写入class信息，反序化时必须读取class信息
		return (T) this.getKryo().readClassAndObject(input);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(InputStream is, Class<T> clazz) throws IOException {
		Input input = new Input(is);
		return (T) this.getKryo().readClassAndObject(input);
	}

	@Override
	public Object deserialize(byte[] bytes) throws IOException {
		Input input = new Input(bytes);
		return this.getKryo().readClassAndObject(input);
	}

	@Override
	public Object deserialize(InputStream is) throws IOException {
		Input input = new Input(is);
		return this.getKryo().readClassAndObject(input);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, T o) throws ClassNotFoundException, IOException {
		return (T) deserialize(bytes, o.getClass());
	}
}
