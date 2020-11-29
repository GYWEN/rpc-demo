package com.boarsoft.rpc.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;

import com.boarsoft.common.Util;
import com.boarsoft.common.annotation.AnnotationProcessor;
import com.boarsoft.rpc.bean.RpcReferenceConfig;
import com.boarsoft.rpc.bean.RpcServiceConfig;
import com.boarsoft.rpc.core.RpcCore;

/**
 * 
 * @author Mac_J
 *
 */
public class RpcAnnotationProcessor implements AnnotationProcessor {
	private static final Logger log = LoggerFactory.getLogger(RpcAnnotationProcessor.class);

	@Override
	public void process(Object bean, String beanName, BeanFactory beanFactory, Class<?> clazz, Annotation annotation) {
		if (annotation instanceof RpcService) {
			if (clazz.getInterfaces().length == 0) {
				throw new IllegalStateException(String.format(//
						"%s must be a class implements 1~N interface", clazz.getName()));
			}
			this.processService(clazz, bean, beanName, beanFactory);
		} else if (annotation instanceof RpcReference) {
			if (!clazz.isInterface()) {
				throw new IllegalStateException(String.format(//
						"@RpcReference is not allowed on %s", clazz.getName()));
			}
			this.processReference(clazz, bean, beanName, beanFactory);
		} else {
			return;
		}
	}

	@Override
	public void process(Object bean, String beanName, BeanFactory beanFactory, Class<?> clazz, Field field,
			Annotation annotation) {
		if (annotation instanceof RpcReference) {
			try {
				this.processReference(clazz, bean, beanName, field, beanFactory);
			} catch (Throwable e) {
				throw new BeanCreationException("Error on process reference", e);
			}
		}
	}

	protected void processReference(Class<?> clazz, Object bean, String beanName, BeanFactory beanFactory) {
		log.info("Process @RpcReference on interface {}", clazz.getName());
		RpcReference a = clazz.getAnnotation(RpcReference.class);
		String id = a.id();
		if (Util.strIsEmpty(id)) {
			id = clazz.getSimpleName();
		}
		String name = a.name();
		if (Util.strIsEmpty(name)) {
			name = clazz.getSimpleName();
		}
		String interfaceName = a.interfaceName();
		if (Util.strIsEmpty(interfaceName)) {
			interfaceName = clazz.getName();
		}
		//
		try {
			RpcReferenceConfig rc = new RpcReferenceConfig(//
					a.group(), name, interfaceName, a.version(), id);
			rc.setAutoMock(a.autoMock());
			rc.setMocker(a.mocker());
			rc.setTimeout(a.timeout());
			rc.setType(rc.getType());
			RpcCore.getCurrentInstance().registReferece(rc);
		} catch (Throwable e) {
			log.error("Error on regist RpcReferce on {}", beanName, e);
		}
	}

	protected void processReference(Class<?> clazz, Object bean, String beanName, Field field, BeanFactory beanFactory)
			throws Throwable {
		String fieldName = field.getName();
		log.info("Process @RpcReference on field {} of bean {}", fieldName, beanName);
		RpcReference a = field.getAnnotation(RpcReference.class);
		String id = a.id();
		if (Util.strIsEmpty(id)) {
			id = fieldName;
		}
		String name = a.name();
		if (Util.strIsEmpty(name)) {
			name = fieldName;
		}
		String interfaceName = a.interfaceName();
		if (Util.strIsEmpty(interfaceName)) {
			Class<?> c = field.getType();
			if (c.isInterface()) {
				interfaceName = c.getName();
			} else {
				throw new IllegalStateException(String.format(//
						"%s is not an interface", fieldName));
			}
		}
		RpcReferenceConfig rc = new RpcReferenceConfig(//
				a.group(), name, interfaceName, a.version(), id);
		rc.setAutoMock(a.autoMock());
		rc.setMocker(a.mocker());
		rc.setTimeout(a.timeout());
		rc.setType(rc.getType());
		RpcCore.getCurrentInstance().registReferece(rc);
		//
		Object obj = beanFactory.getBean(id);
		boolean acc = field.isAccessible();
		try {
			field.setAccessible(true);
			field.set(bean, obj);
		} finally {
			field.setAccessible(acc);
		}
	}

	protected void processService(Class<?> clazz, Object bean, String beanName, BeanFactory beanFactory) {
		log.info("Process @RpcService on bean {}", beanName);
		RpcService a = clazz.getAnnotation(RpcService.class);
		String name = a.name();
		if (Util.strIsEmpty(name)) {
			name = beanName;
		}
		String interfaceName = a.interfaceName();
		if (Util.strIsEmpty(interfaceName)) {
			Class<?>[] ia = clazz.getInterfaces();
			if (ia.length == 0) {
				throw new IllegalStateException(String.format(//
						"%s has no interface", beanName));
			}
			interfaceName = ia[0].getName();
		}
		try {
			RpcServiceConfig sc = new RpcServiceConfig(//
					a.group(), name, interfaceName, a.version(), beanName);
			sc.setTimeout(a.timeout());
			sc.setType(a.type());
			sc.setMocker(a.mocker());
			sc.setAutoMock(a.autoMock());
			sc.setEnable(a.enable());
			sc.setUri(a.uri());
			sc.setId(a.id());
			log.warn("Regist RpcSerivce {} via annotation", sc);
			RpcCore rpcCore = beanFactory.getBean("rpcCore", RpcCore.class);
			rpcCore.registService(sc);
		} catch (Throwable e) {
			log.error("Error on regist RpcSerivce {}", beanName, e);
		}
	}
}
