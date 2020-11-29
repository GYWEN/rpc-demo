package com.boarsoft.rpc.bean;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.boarsoft.common.Util;
import com.boarsoft.common.util.ReflectUtil;
import com.boarsoft.rpc.core.RpcContext;

public class RpcMethodConfig implements Serializable {
	private static final long serialVersionUID = -238430885029993755L;
	private static final Logger log = LoggerFactory.getLogger(RpcMethodConfig.class);

	/** 提交注册表 */
	public static final int ID_POP_REGISTRY = 0;
	/** 绑定连接 */
	public static final int ID_BIND_LINK = 1;
	/** 注册表推送 */
	public static final int ID_PUSH_REGISTRY = 2;
	/** 发送心跳 */
	public static final int ID_HEART_BEAT = 3;
	/** 停机通知 */
	public static final int ID_SHUTTING_DOWN = 4;
	/** 比较两个节点间的注册表版本号 */
	@Deprecated
	public static final int ID_CHECK_SYNC = 5;
	/** 节点断线通知 */
	public static final int ID_NODE_OFF = 6;
	/** 废弃的设计，用于检查节点的健康状态，暂时保留 */
	@Deprecated
	public static final int ID_CHECK_NODE = 7;
	/** 在本地放开对某个远程节点的指定服务的访问 */
	public static final int ID_ENABLE_PROVIDER = 8;
	/** 在本地禁止对某个远程节点的某个服务的访问 */
	public static final int ID_DISABLE_PROVIDER = 9;
	/** 在本地放开对某个远程节点的所有服务的访问 */
	public static final int ID_ENABLE_PROVIDERS = 10;
	/** 在本地禁止对某个远程节点的所有服务的访问 */
	public static final int ID_DISABLE_PROVIDERS = 11;
	/** 在本地放开对所有远程节点的某个服务的访问 */
	public static final int ID_ENABLE_SERVICE = 12;
	/** 在本地禁止对所有远程节点的某个服务的访问 */
	public static final int ID_DISABLE_SERVICE = 13;
	/** 返回本地所有注册表信息 */
	public static final short ID_GET_INFO = 14;

	/** SC：本地：同步调起，阻塞以等待结果；远程：异步执行，返回结果 */
	public static final short TYPE_SYNC_CALL = 0;
	/** AC，本地：同步调起，返回Future；远程：异步执行，返回结果 */
	public static final short TYPE_ASYNC_CALL = 1;
	/** SN，本地：同步调起，阻塞以等待应答；远程：异步执行，回声应答 */
	public static final short TYPE_SYNC_NOTICE = 2;
	/** AN，本地：同步调起，返回Future；远程：异步执行，回声应答 */
	public static final short TYPE_ASYNC_NOTICE = 3;
	/** SB，本地：依次调用所有服务提供者，等待拿到的所有的Future结束； 远程：异步执行，返回结果 */
	public static final short TYPE_SYNC_BROADCAST = 4;
	/** AB，本地：依次调用所有服务提供者，直接返回拿到的所有的Future；远程：异步执行，返回结果 */
	public static final short TYPE_ASYNC_BROADCAST = 5;
	/** BN，本地：依次调用所有服务提供者，直接返回拿到的所有的Future；远程：异步执行，回声应答 */
	public static final short TYPE_BROADCAST_NOTICE = 6;

	/** 方法签名 */
	protected String sign;
	/** 接口签名 + 方法签名 */
	protected String key;
	protected int timeout = 30000;
	protected RpcFaceConfig faceConfig;
	protected Integer relativeId;
	protected int failover = 0;
	// protected String filters;
	/** 这是方法的调用方式，不是请求响应标识 */
	protected short type = TYPE_SYNC_CALL;
	protected String callback;
	protected int protocol = 0; // hessian
	protected boolean autoMock = false;
	protected String uri = null;
	
	/** 需要 java8 的 javac -parameters */
	protected transient Parameter[] parameters;

	@Deprecated
	public RpcMethodConfig() {
		// 仅为Kryo序列化保留
	}

	public RpcMethodConfig(RpcFaceConfig faceConfig, Method method) {
		this.faceConfig = faceConfig;
		this.sign = ReflectUtil.getMethodSign(method);
		this.key = new StringBuilder(faceConfig.getSign()).append("/").append(this.sign).toString();
		// 如果方法有RpcMethod注解，表示是一个内部方法，直接使用ID
		if (method.isAnnotationPresent(RpcMethod.class)) {
			RpcMethod m = method.getAnnotation(RpcMethod.class);
			this.relativeId = m.id();
			this.timeout = m.timeout();
			this.type = m.type();
			this.autoMock = m.autoMock();
			this.uri = m.uri();
			RpcContext.putMyMethodId(m.id(), this);
		} else {
			this.setTimeout(faceConfig.getTimeout());
			this.setType(faceConfig.getType());
			this.autoMock = faceConfig.isAutoMock();
		}
		if (Util.strIsEmpty(this.uri)) {
			this.uri = "/".concat(method.getName());
		}
		if (method.isAnnotationPresent(RpcUri.class)) {
			RpcUri a = method.getAnnotation(RpcUri.class);
			this.uri = a.value();
		}
		this.parameters = method.getParameters();
		log.debug("Create config of method {}", this.key);
	}

	public String getMocker() {
		return faceConfig.getMocker();
	}

	public String getKey() {
		return key;
	}

	public String toString() {
		return this.key;
	}

	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(int timeout) {
		if (timeout < 1) {
			throw new IllegalArgumentException("Timeout must > 0");
		}
		this.timeout = timeout;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public RpcFaceConfig getFaceConfig() {
		return this.faceConfig;
	}

	public void setFaceConfig(RpcFaceConfig faceConfig) {
		this.faceConfig = faceConfig;
	}

	public Integer getRelativeId() {
		return this.relativeId;
	}

	public void setRelativeId(Integer relativeId) {
		if (relativeId == null) {
			throw new IllegalArgumentException("Relative id can not be null");
		}
		this.relativeId = relativeId;
	}

	public int getFailover() {
		return failover;
	}

	public void setFailover(int failover) {
		if (failover < 0) {
			throw new IllegalArgumentException("RpcMethodConfig.failover must >= 0");
		}
		this.failover = failover;
	}

	public void setType(String type) {
		if ("SC".equalsIgnoreCase(type)) {
			this.type = TYPE_SYNC_CALL;
		} else if ("AC".equalsIgnoreCase(type)) {
			this.type = TYPE_ASYNC_CALL;
		} else if ("SN".equalsIgnoreCase(type)) {
			this.type = TYPE_SYNC_NOTICE;
		} else if ("AN".equalsIgnoreCase(type)) {
			this.type = TYPE_ASYNC_NOTICE;
		} else if ("SB".equalsIgnoreCase(type)) {
			this.type = TYPE_SYNC_BROADCAST;
		} else if ("AB".equalsIgnoreCase(type)) {
			this.type = TYPE_ASYNC_BROADCAST;
		} else if ("BN".equalsIgnoreCase(type)) {
			this.type = TYPE_BROADCAST_NOTICE;
		}
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getCallback() {
		return callback;
	}

	public void setCallback(String callback) {
		this.callback = callback;
	}

	public int getProtocol() {
		return protocol;
	}

	public void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public boolean isAutoMock() {
		return autoMock;
	}

	public void setAutoMock(boolean autoMock) {
		this.autoMock = autoMock;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public Parameter[] getParameters() {
		return parameters;
	}

	public void setParameters(Parameter[] parameters) {
		this.parameters = parameters;
	}

}
