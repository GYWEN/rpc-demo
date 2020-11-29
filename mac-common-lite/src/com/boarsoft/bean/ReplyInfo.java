package com.boarsoft.bean;

import java.io.Serializable;

public class ReplyInfo<T> implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final ReplyInfo<Object> SUCCESS = new ReplyInfo<Object>();
	public static final ReplyInfo<Object> FAILED = new ReplyInfo<Object>(null);

//	/** 处理/受理成功 */
//	public static final String OK = "Msg.info.success";
//	/** 请求不合法 */
//	public static final String BAD_REQUEST = "Msg.info.badRequest";
//	/** 请先登录 */
//	public static final String NEED_LOGIN = "Msg.info.needLogin";
//	/** 未授权/越权访问 */
//	public static final String UNAUTHORIZED = "Msg.info.unauthorized";
//	/** 用户有权限，但服务被（暂时）禁止访问 */
//	public static final String FORBIDDEN = "Msg.info.forbidden";
//	/** 用户有权限，但服务（暂时）不可用 */
//	public static final String UNAVAILABLE = "Msg.info.unavailable";
//	/** 不支持的请求（客户端版本过期） */
//	public static final String UNSUPPORTED = "Msg.info.unsupported";
//	/** 请求的数据未找到 */
//	public static final String NOT_FOUND = "Msg.info.notFound";

//	/** 内部服务超时 */
//	public static final String INTERAL_TIMEOUT = "Msg.error.timeout";
//	/** 内部服务错误 */
//	public static final String INTERNAL_ERROR = "Msg.error.internal";

	protected boolean success = true;
	protected T data = null;
	protected String[] params;

	public ReplyInfo() {
		this.success = true;
	}

	public ReplyInfo(T data) {
		this.success = false;
		this.data = data;
	}

	public ReplyInfo(T data, String[] params) {
		this.success = false;
		this.data = data;
		this.params = params;
	}

	public ReplyInfo(T data, String param) {
		this.success = false;
		this.data = data;
		this.params = new String[] { param };
	}

	public ReplyInfo(boolean success, T data) {
		this.success = success;
		this.data = data;
	}

	public ReplyInfo(boolean success, T data, String param) {
		this.success = success;
		this.data = data;
		this.params = new String[] { param };
	}

	public ReplyInfo(boolean success, T data, String[] params) {
		this.success = success;
		this.data = data;
		this.params = params;
	}

	public static <E> ReplyInfo<E> success(E data) {
		return new ReplyInfo<E>(true, data);
	}

	public static <E> ReplyInfo<E> success(E data, String param) {
		return new ReplyInfo<E>(true, data, param);
	}

	public static <E> ReplyInfo<E> success(E data, String[] params) {
		return new ReplyInfo<E>(true, data, params);
	}

	public static <E> ReplyInfo<E> error(E error) {
		return new ReplyInfo<E>(false, error);
	}

	public static <E> ReplyInfo<E> error(E error, String param) {
		return new ReplyInfo<E>(false, error, param);
	}

	public static <E> ReplyInfo<E> error(E error, String[] params) {
		return new ReplyInfo<E>(false, error, params);
	}

	public String[] getParams() {
		return params;
	}

	public void setParams(String[] params) {
		this.params = params;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}
}