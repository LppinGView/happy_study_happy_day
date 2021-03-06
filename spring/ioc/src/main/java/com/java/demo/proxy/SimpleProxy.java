package com.java.demo.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * jdk dynamic proxy
 */
public class SimpleProxy {

	private Object target;

	public SimpleProxy(Object target){
		this.target = target;
	}

	public Object getProxyInstance(){
		return Proxy.newProxyInstance(
				target.getClass().getClassLoader(),
				target.getClass().getInterfaces(),
				(Object proxy, Method method, Object[] args) -> {
					if (Objects.nonNull(method.getAnnotation(com.java.demo.annotion.Proxy.class))){
						if (method.getName().equals("register")){
							System.out.println("register start...");
							method.invoke(target, args);
							System.out.println("register end...");
							return null;
						}else if (method.getName().equals("getEarlyBeanReference")){
							System.out.println("proxy start...");
							method.invoke(target, args);
							System.out.println("proxy end...");
							return proxy;
						}
					} else {
						//if method have return, here you should return
						return method.invoke(target, args);
					}
					return null;
				}
		);
	}

}
