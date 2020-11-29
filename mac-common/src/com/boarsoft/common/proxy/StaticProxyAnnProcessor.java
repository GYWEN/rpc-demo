package com.boarsoft.common.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;

import com.boarsoft.common.Util;
import com.boarsoft.common.annotation.AnnotationProcessor;

/**
 * 
 * @author Mac_J
 *
 */
public class StaticProxyAnnProcessor implements AnnotationProcessor {
	private static final Logger log = LoggerFactory.getLogger(StaticProxyAnnProcessor.class);

	@Override
	public void process(Object bean, String beanName, BeanFactory beanFactory, Class<?> clazz, Annotation a) {
		// Nothing to do
	}

	@Override
	public void process(Object bean, String beanName, BeanFactory beanFactory, Class<?> clazz, Field field,
			Annotation annotation) {
		if (!(annotation instanceof StaticProxyAnn)) {
			return;
		}
		//
		StaticProxyAnn a = (StaticProxyAnn) annotation;
		String prototype = a.value();
		if (Util.strIsEmpty(prototype)) {
			prototype = field.getName();
		}
		// prototype
		StaticProxy proxy = beanFactory.getBean(prototype, StaticProxy.class);
		Object target = null;
		boolean acc = field.isAccessible();
		try {
			field.setAccessible(true);
			target = field.get(bean);
			if (target == null) {
				log.error("{} is null", field);
				return;
			}
			proxy.setTarget(target);
			field.set(bean, proxy);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.error("Can not access field {}", field, e);
		} finally {
			field.setAccessible(acc);
		}
	}

}
