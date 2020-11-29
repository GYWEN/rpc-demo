package com.boarsoft.rpc.bean;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ java.lang.annotation.ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RpcMethod {
	/**
	 * 方法ID
	 * 
	 * @return
	 */
	int id();

	/**
	 * 超时时间，单位毫秒
	 * 
	 * @return
	 */
	int timeout() default 30000;

	/**
	 * 方法调用类型<br>
	 * RpcMethodConfig.TYPE_XXX
	 * 
	 * @return
	 */
	short type() default RpcMethodConfig.TYPE_SYNC_CALL;

	/**
	 * 是否在异常时自动调用mocker
	 * 
	 * @return
	 */
	boolean autoMock() default false;

	/**
	 * 用于简化此方法的定位
	 * 
	 * @return
	 */
	String uri() default "";
}