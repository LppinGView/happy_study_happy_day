package com.java.demo.proxy;

import com.java.demo.annotion.Proxy;
import com.java.demo.bean.InstanceA;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.SmartInstantiationAwareBeanPostProcessor;

public class MyProxyBeanPostProcessor implements SmartInstantiationAwareBeanPostProcessor {

    @Proxy
    public Object getEarlyBeanReference(String beanName, Object bean) throws BeansException {
        //表示被切点命中了
        if (bean instanceof InstanceA){
            System.out.println("创建"+beanName+"的代理对象!");
            System.out.println("创建代理对象结束!");
        }
        return bean;
    }
}
