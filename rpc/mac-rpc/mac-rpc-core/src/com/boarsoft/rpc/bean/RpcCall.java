package com.boarsoft.rpc.bean;

import java.io.Serializable;
import java.util.Properties;

/**
 * RPC调用请求或响应对象
 * 
 * @author Mac_J
 *
 */
public class RpcCall implements Serializable {
	private static final long serialVersionUID = 2552653676124740191L;

	/** RPC请求 */
	public static final int TYPE_REQUEST = 0;
	/** RPC响应 */
	public static final int TYPE_RESPONSE = 1;

	/** 用于无参方法和RPC响应对象 */
	public static final Object[] EMPTY_ARGS = new Object[] {};

	/** 0：请求，1：响应 */
	protected int type = -1;
	/** 方法ID */
	protected int methodId;
	/** 方法调用参数 */
	protected Object[] arguments;
	/** 方法执行序号 */
	protected long methodExeNo;
	/** 数据包主内容的编解码或序列化协议，比如：Hessian、Kryo、JSON */
	protected int protocol;
	/** 方法调用的返回值 */
	protected Object result;
	/** 方法抛出的异常 */
	protected Throwable throwable;
	/** 方法执行完成后，希望调起的（调用方）方法ID */
	protected Integer callback;
	/** 本次调用流水编号 */
	protected String traceId;
	/** 希望额外传递的附件 */
	protected Properties attachments;

	public RpcCall() {
	}

	public RpcCall(int protocol, int type, int methodId, long methodExeNo, Object[] args) {
		this.protocol = protocol;
		this.type = type;
		this.methodId = methodId;
		this.methodExeNo = methodExeNo;
		this.arguments = args;
	}

	public String toString() {
		return new StringBuilder().append(type).append(".")//
				.append(methodId).append(".").append(methodExeNo).toString();
	}

	public int getType() {
		return this.type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public Object[] getArguments() {
		return this.arguments;
	}

	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}

	public Object getResult() {
		return this.result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public Throwable getThrowable() {
		return this.throwable;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public long getMethodExeNo() {
		return this.methodExeNo;
	}

	public void setMethodExeNo(long methodExeNo) {
		this.methodExeNo = methodExeNo;
	}

	public int getMethodId() {
		return this.methodId;
	}

	public void setMethodId(int methodId) {
		this.methodId = methodId;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public Integer getCallback() {
		return callback;
	}

	public void setCallback(Integer callback) {
		this.callback = callback;
	}

	public Properties getAttachments() {
		return attachments;
	}

	public void setAttachments(Properties attachments) {
		this.attachments = attachments;
	}
}
