package com.boarsoft.common.util;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

public class BeanUtil {
	private static final Logger log = LoggerFactory.getLogger(BeanUtil.class);

	/**
	 * 用一个对象填充一个对象
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	public static <T> T transfer(Object source, Object target) {
		return transferWith(source, target, null);
	}

	/**
	 * 用一个对象填充一个对象，只传输指定的字段
	 * 
	 * @param source
	 * @param target
	 * @param includes
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T transferWith(Object source, Object target, Set<String> includes) {
		BeanWrapper bws = new BeanWrapperImpl(source);
		BeanWrapper bwt = new BeanWrapperImpl(target);
		PropertyDescriptor[] pds = bws.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			String s = pd.getName();
			if (includes == null || includes.contains(s)) {
				if (bws.isReadableProperty(s) && bwt.isWritableProperty(s)) {
					bwt.setPropertyValue(s, bws.getPropertyValue(s));
				}
			}
		}
		return (T) target;
	}

	/**
	 * 用一个对象填充一个对象，但排除掉指定的字段
	 * 
	 * @param source
	 * @param target
	 * @param excludes
	 * @return
	 */
	public static Object transferWithout(Object source, Object target, Set<String> excludes) {
		BeanWrapper bws = new BeanWrapperImpl(source);
		BeanWrapper bwt = new BeanWrapperImpl(target);
		PropertyDescriptor[] pds = bws.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			String s = pd.getName();
			if (excludes == null || !excludes.contains(s)) {
				if (bws.isReadableProperty(s) && bwt.isWritableProperty(s)) {
					bwt.setPropertyValue(s, bws.getPropertyValue(s));
				}
			}
		}
		return target;
	}

	/**
	 * 根据一个对象创建一个新的对象，被创建的对象类型需要有无参的构造函数<br>
	 * 
	 * 
	 * @param source
	 * @param targetClass
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> T transfer(Object source, Class clazz) {
		T result = null;
		try {
			result = (T) clazz.newInstance();
			return transfer(source, result);
		} catch (InstantiationException e) {
			log.error("", e);
		} catch (IllegalAccessException e) {
			log.error("", e);
		}
		return result;
	}

	/**
	 * 根据一个对象创建一个新的对象，被创建的对象类型需要有无参的构造函数<br>
	 * 只传输指定的属性
	 * 
	 * @param source
	 * @param targetClass
	 * @param includes
	 *            要包括的属性列表，为空表示包括全部
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object transferWith(Object source, Class targetClass, Set<String> includes) {
		Object result = null;
		try {
			result = targetClass.newInstance();
			return transferWith(source, result, includes);
		} catch (InstantiationException e) {
			log.error("", e);
		} catch (IllegalAccessException e) {
			log.error("", e);
		}
		return result;
	}

	/**
	 * 根据一个对象创建一个新的对象，被创建的对象类型需要有无参的构造函数<br>
	 * 排除掉指定的属性
	 * 
	 * @param source
	 * @param targetClass
	 * @param excludes
	 *            要排除的属性列表，为空表示不排除
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static Object transferWithout(Object source, Class targetClass, Set<String> excludes) {
		Object result = null;
		try {
			result = targetClass.newInstance();
			return transferWithout(source, result, excludes);
		} catch (InstantiationException e) {
			log.error("", e);
		} catch (IllegalAccessException e) {
			log.error("", e);
		}
		return result;
	}

	/**
	 * 将map中的值作为目标对象的属性填入
	 * 
	 * @param o
	 * @param map
	 * @return
	 */
	public static void fillObjectWithMap(Object o, Map<String, Object> map) {
		BeanWrapper bw = new BeanWrapperImpl(o);
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (int i = 0; i < pds.length; i++) {
			String s = pds[i].getName();
			if (bw.isWritableProperty(s) && map.containsKey(s)) {
				if (map.get(s) != null) {
					bw.setPropertyValue(s, map.get(s));
				}
			}
		}
	}

	/**
	 * 获取对象的所有或指定属性，填入map<br>
	 * 如果 map 为空，则表示获取所有可读属性，返之表示只获取map中有的属性
	 * 
	 * @param map
	 * @param o
	 * @return
	 */
	public static void fillMapWithObject(Map<String, Object> map, Object o) {
		BeanWrapper bw = new BeanWrapperImpl(o);
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		boolean b = map.isEmpty();
		for (int i = 0; i < pds.length; i++) {
			String s = pds[i].getName();
			if (b || map.containsKey(s)) {
				map.put(s, bw.getPropertyValue(s));
			}
		}
	}

	public <T, E> List<T> copy(List<E> list, Class<T> clazz) {
		List<T> targetList = new ArrayList<T>();
		try {
			for (E e : list) {
				T target = clazz.newInstance();
				BeanUtils.copyProperties(e, target);
				targetList.add(target);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return targetList;
	}
}
