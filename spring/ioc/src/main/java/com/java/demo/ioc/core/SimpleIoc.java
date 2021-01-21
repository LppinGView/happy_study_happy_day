package com.java.demo.ioc.core;

import com.java.demo.annotion.Proxy;

public interface SimpleIoc {

	@Proxy
	void register(Object bean);

	Object getBean(String name);

	<T> T getBean(Class<T> clzss);

	void close();
}
