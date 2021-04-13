package com.leo.java.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * synchronized 用于对资源进行加锁操作，基于监听器的 管程模型
 * 1.用于代码块
 * 2.用于方法
 *
 * 不足点：
 *
 * 是可重入锁、非公平锁
 */
public class Lock {

    private static int a = 100;

    //修饰非静态方法
    synchronized void foo(){
        //临界区
    }

    //修饰静态方法
    synchronized static void bar(){
        //临界区
    }

    private final Object lockA = new Object();
    // 1.用于代码块
    void baz() {
        for (int i = 0; i < 100; i++) {
            synchronized (lockA){
                //临界区
                a++;
                System.out.println("synchronized可重入");
            }
        }

        synchronized (Lock.class){
            //临界区
        }
    }

    public static void main(String[] args) {
        Lock lock = new Lock();
        // 4.可重入
        lock.baz();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("t1 >>"+lock.a);
            }
        });
        MyThread t2 = new MyThread();
        t1.start();
        t2.start();

        System.out.println("获取a的值");
        System.out.println(lock.a);
    }

    static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("t2 >>"+ a);
        }
    }

}

class NewLock{

    public static void main(String[] args) throws Exception {
        // 1.初始化选择公平锁、非公平锁   不指定的话为非公平锁
        ReentrantLock lock = new ReentrantLock(true);
        // 2.可用于代码块
        lock.lock();
        try {
            try {
                // 3.支持多种加锁方式，比较灵活; 具有可重入特性
                if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {}
            }finally {
                // 4.手动释放锁
                lock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }
}
