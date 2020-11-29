package com.boarsoft.common.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.springframework.beans.factory.BeanFactory;

/**
 * SAGA Transaction Wrapper Process Handler Annotation Processor
 * 
 * @author Mac_J
 *
 */
public interface AnnotationProcessor {

	/**
	 * 
	 * @param bean
	 * @param beanName
	 * @param beanFactory
	 * @param clazz
	 * @param annotation
	 */
	void process(Object bean, String beanName, BeanFactory beanFactory, Class<?> clazz, Annotation annotation);

	/**
	 * 
	 * @param bean
	 * @param beanName
	 * @param beanFactory
	 * @param clazz
	 * @param field
	 * @param annotation
	 */
	void process(Object bean, String beanName, BeanFactory beanFactory, Class<?> clazz, Field field, Annotation annotation);

}
