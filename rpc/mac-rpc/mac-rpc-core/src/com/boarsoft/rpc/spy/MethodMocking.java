package com.boarsoft.rpc.spy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodMocking {
	protected String key;
	protected Object mocker;
	protected Method method;

	public MethodMocking(String key, Object mocker, Method method) {
		this.key = key;
		this.mocker = mocker;
		this.method = method;
	}

	public Object invoke(Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return method.invoke(mocker, args);
	}

	public String toString() {
		return method.toString();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getMocker() {
		return mocker;
	}

	public void setMocker(Object mocker) {
		this.mocker = mocker;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}
}
