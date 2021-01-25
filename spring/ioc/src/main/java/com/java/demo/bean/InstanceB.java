package com.java.demo.bean;

import com.java.demo.annotion.Autowired;

public class InstanceB {

    @Autowired
    private InstanceA instanceA;

    public InstanceB(){
        System.out.println("实例化B");
    }
}
