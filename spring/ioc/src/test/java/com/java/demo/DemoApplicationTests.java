package com.java.demo;

import com.java.demo.bean.People;
import com.java.demo.proxy.SimpleProxy;
import com.java.demo.ioc.core.v1.DefaultSimpleIoc;
import com.java.demo.ioc.core.v1.SimpleIoc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Test
    void hashMap(){

        Map map = new HashMap();
        map.put("leo","1");
        map.put("leo", "1");
        List<String> list = new ArrayList<>();
        list.add("1");
        list.add("1");
        list.add("2");
        list.stream().collect(Collectors.toMap(it->it,it->it));

        System.out.println(map.toString());

    }

}
