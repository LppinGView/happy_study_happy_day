package com.lpp.demo.scalable;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 此为一连接一线程模型
 * 缺点：当负载增多时，服务端线程数也会很多，线程占用内存过多，且切换线程开销过多，系统效率不高
 *
 * 怎么做到 可扩展目标：
 *  1.当负载增多时，服务能够优雅降解，从而不影响其他连接
 *  2.当资源增多时，持续改善服务性能
 *  3.可用和性能目标：低延时、满足峰值要求、服务质量可调
 *
 * 分而治之：
 *  1.拆分程序为多个小任务。每个任务都是非阻塞执行
 *  2.只有当任务可以被执行时，才去执行它。
 *  3.java NIO的支持，非阻塞的读写，selector选择器
 *  4.一系列事件驱动设计
 *
 * 事件驱动设计
 *  1.很少的资源投入：不需要一个连接一个线程
 *  2.更少的性能开销：线程的上下文切换、更少的锁定
 *  3.但是调度可能更慢：必须手动的把操作绑定到事件
 *  4.更难编程：必须分解成简单的非阻塞动作（不能消除所有的阻塞：GC、内存页缺失中断等等）
 *  5.必须保持追踪服务器逻辑状态
 *
 */

public class Server implements Runnable{

    private int PORT = 8888;
    private static int MAX_INPUT = 100;

    @Override
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            while (!Thread.interrupted()) {
                new Thread(new Handler(serverSocket.accept())).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class Handler implements Runnable {
        private Socket socket;

        public Handler (Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            byte[] input = new byte[MAX_INPUT];
            try {
                socket.getInputStream().read(input);
                byte[] output = process(input);
                socket.getOutputStream().write(output);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private byte[] process(byte[] input) {
            return null;
        }
    }
}
