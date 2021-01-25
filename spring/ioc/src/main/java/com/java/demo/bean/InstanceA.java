package com.java.demo.bean;

import com.java.demo.annotion.Autowired;

public class InstanceA {

//    private Integer i = 12;

    @Autowired
    private InstanceB instanceB;

    public InstanceA(){
        System.out.println("实例化A");
    }

//    public InstanceA(Integer i){
//        this.i = i;
//    }
//
//    public void setI(Integer i){
//        this.i = i;
//    }
//
//    public Integer getI(){
//        return this.i;
//    }

    public void say(){
        System.out.println("i am a");
    }
}
