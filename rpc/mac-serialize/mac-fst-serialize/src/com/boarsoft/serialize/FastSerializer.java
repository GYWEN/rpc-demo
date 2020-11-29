package com.boarsoft.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
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

import org.nustaq.serialization.FSTConfiguration;

public class FastSerializer implements ObjectSerializer {
	/** */
	public static final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

	static {
		// copy from dubbo
		conf.registerClass(GregorianCalendar.class);
		// 不能注册这个类，会报NoClassDefException
		// conf.registerClass(InvocationHandler.class);
		conf.registerClass(BigDecimal.class);
		conf.registerClass(BigInteger.class);
		conf.registerClass(Pattern.class);
		conf.registerClass(BitSet.class);
		conf.registerClass(URI.class);
		conf.registerClass(UUID.class);
		conf.registerClass(HashMap.class);
		conf.registerClass(ArrayList.class);
		conf.registerClass(LinkedList.class);
		conf.registerClass(HashSet.class);
		conf.registerClass(TreeSet.class);
		conf.registerClass(Hashtable.class);
		conf.registerClass(Date.class);
		conf.registerClass(Calendar.class);
		conf.registerClass(ConcurrentHashMap.class);
		conf.registerClass(SimpleDateFormat.class);
		conf.registerClass(Vector.class);
		conf.registerClass(BitSet.class);
		conf.registerClass(StringBuffer.class);
		conf.registerClass(StringBuilder.class);
		conf.registerClass(Object.class);
		conf.registerClass(Object[].class);
		conf.registerClass(String[].class);
		conf.registerClass(byte[].class);
		conf.registerClass(char[].class);
		conf.registerClass(int[].class);
		conf.registerClass(float[].class);
		conf.registerClass(double[].class);
		// added
		conf.registerClass(Throwable.class);
		conf.registerClass(Exception.class);
	}

	@Override
	public byte[] serialize(Object obj) throws IOException {
		return conf.asByteArray((Serializable) obj);
	}

	@Override
	public void serialize(Object obj, OutputStream os) throws IOException {
		conf.getObjectOutput().writeObject(os);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException, ClassNotFoundException {
		return (T) conf.getObjectInput(bytes).readObject();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(InputStream is, Class<T> clazz) throws IOException, ClassNotFoundException {
		return (T) this.deserialize(is);
	}

	@Override
	public Object deserialize(InputStream is) throws IOException, ClassNotFoundException {
		return conf.getObjectInput(is).readObject();
	}

	@Override
	public Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
		return conf.getObjectInput(bytes).readObject();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T deserialize(byte[] bytes, T o) throws ClassNotFoundException, IOException {
		return (T) deserialize(bytes, o.getClass());
	}
}
