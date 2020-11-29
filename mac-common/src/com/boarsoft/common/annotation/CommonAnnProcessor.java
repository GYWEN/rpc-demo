package com.boarsoft.common.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessorAdapter;

/**
 * 通用自定义（类、属性）注解解析
 * 
 * @author Mac_J
 *
 */
public class CommonAnnProcessor extends InstantiationAwareBeanPostProcessorAdapter implements BeanFactoryAware {
	/** */
	protected BeanFactory beanFactory;
	/** */
	protected List<AnnotationProcessor> processorList;

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		Class<?> clazz = bean.getClass();
		Annotation[] caa = clazz.getAnnotations();
		for (Annotation a : caa) {
			for (AnnotationProcessor ap : processorList) {
				ap.process(bean, beanName, beanFactory, clazz, a);
			}
		}
		Field[] fields = clazz.getDeclaredFields();
		for (Field f : fields) {
			Annotation[] faa = f.getAnnotations();
			for (Annotation a : faa) {
				for (AnnotationProcessor ap : processorList) {
					ap.process(bean, beanName, beanFactory, clazz, f, a);
				}
			}
		}
		return bean;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public BeanFactory getBeanFactory() {
		return beanFactory;
	}

	public List<AnnotationProcessor> getProcessorList() {
		return processorList;
	}

	public void setProcessorList(List<AnnotationProcessor> processorList) {
		this.processorList = processorList;
	}

}
