package com.java.demo.ioc.core;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * simple IOC container
 * @author by lpp
 *
 * Provides some method to manage beans and destroy the bean.
 * such as getBean, register, release
 */
public class DefaultSimpleIoc implements SimpleIoc {
	private static Map<String, Object> beans = new ConcurrentHashMap<>(16);

	/**
	 * register bean
	 * we can do something here, such as Intercept, proxy , balaba...
	 * @param bean
	 */
	public void register(Object bean){
		if (Objects.isNull(bean)) return;
		//bean name must be lowercase
		beans.put(bean.getClass().getSimpleName().toLowerCase(), bean);
	}

	/**
     * get the bean by name
	 * @param name
     * @return
     */
	public Object getBean(String name){
		return beans.get(name);
	}

	/**
	 * get the specific bean by class type
	 * @param clzss
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> clzss){
		return (T) getBean(clzss.getSimpleName().toLowerCase());
	}

	/**
	 * destory the IOC container
	 */
	public void close(){
		beans = null;
		System.out.println("release all beans success.");
	}
}
