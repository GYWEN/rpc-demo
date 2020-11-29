package com.boarsoft.rpc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author Mac_J
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface RpcService {

	String id() default "";

	String group() default "default";

	String name() default "";

	String version() default "1.0.0";

	String interfaceName() default "";

	int timeout() default 30000;

	String type() default "SC";

	String mocker() default "";

	boolean autoMock() default false;

	String ref() default "";

	boolean enable() default true;

	String uri() default "";

}