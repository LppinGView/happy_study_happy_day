package com.java.demo;

import com.java.demo.bean.People;
import com.java.demo.proxy.SimpleProxy;
import com.java.demo.ioc.core.DefaultSimpleIoc;
import com.java.demo.ioc.core.SimpleIoc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DemoApplicationTests {

    @Test
    void IocTest() {
        SimpleIoc container = new DefaultSimpleIoc();
        //create proxy
        SimpleProxy proxy = new SimpleProxy(container);
        SimpleIoc containerProxy = (SimpleIoc) proxy.getProxyInstance();
        //constructor injection
        People people = new People("leo", 18, "");
        //register this bean
        containerProxy.register(people);
        //test getBean with name or classType
        People peo = (People) containerProxy.getBean("people");
        People peop = containerProxy.getBean(People.class);
        peo.sayHello();
        peop.sayHello();

        //release container
        container.close();
    }

}
