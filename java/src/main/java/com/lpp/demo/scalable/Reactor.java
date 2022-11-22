package com.lpp.demo.scalable;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 1.Reactor responds to IO events by dispatching the appropriate handler
 * 2.Handlers perform non-blocking actions
 * 3.Manage by binding handlers to events
 *
 * java NIO 的支持
 * 1.Channels：Connections to files, sockets etc that support non-blocking reads
 * 2.Buffers：Array-like objects that can be directly read or written by Channels
 * 3.Selectors：Tell which of a set of Channels have IO events
 * 4.SelectionKeys：Maintain IO event status and bindings
 *
 *
 * 多线程设计
 * 主要应对多核cpu，能够充分利用机器性能
 *
 * 分：工作线程、反应器线程
 *  1.工作线程：
 *      反应器应该快速触发处理器（由于是单线程，处理器处理过程减缓了反应器）
 *      将非IO处理卸载到其他线程（加快处理器处理过程）
 *
 *  2.反应器线程：
 *      在处理IO时，反应器线程工作要饱和
 *      能够将负载分配给其他反应器（负载平衡以匹配CPU和IO速率，不同机器分配不同请求量）
 *
 *
 */
public class Reactor implements Runnable {

    final Selector selector;
    final ServerSocketChannel serverSocket;
    final int MAXIN = 100;
    final int MAXOUT = 100;

    //1.setup
    public Reactor(int port) throws IOException {
        selector = Selector.open(); //开启选择器
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        sk.attach(new Acceptor()); //装载附件，获取可以通过attachment获取，这里也类似注册
    }

    //2.Dispatch Loop
    @Override
    public void run() {  // normally in a newThread? what is mean?
        try {
            System.out.println("reactor starting");
            while (!Thread.interrupted()) {
                //Selects a set of keys whose corresponding channels are ready for I/O operations.
                selector.select();
                Set<SelectionKey> selected = selector.selectedKeys();
                Iterator<SelectionKey> it = selected.iterator();
                while (it.hasNext()) {
                    dispatch(it.next());
                }
//                selected.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 分发已经准备待续的事件（accept, read, send）等等
     * @param sk
     */
    void dispatch (SelectionKey sk) {
        Runnable r = (Runnable) sk.attachment();
        if (r != null) {
            r.run();
        }
    }

    //3.Acceptor
    class Acceptor implements Runnable {

        @Override
        public void run() {
            try {
                SocketChannel c = serverSocket.accept();
                if (c != null) {
                    new Handler(selector, c);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //4.Handler Setup
    final class Handler implements Runnable {

        final SocketChannel socket; //用于处理业务逻辑 非阻塞读写操作
        final SelectionKey sk; //获取当前的绑定事件和状态
//        final Selector sel;
        ByteBuffer input = ByteBuffer.allocate(MAXIN); //用于channel的读
        ByteBuffer output = ByteBuffer.allocate(MAXOUT);//用于channel的写
        static final int READING = 0, SENDING = 1, PROCESSING = 3;
        int state = READING;

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                3,
                5,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(500),
                new CustomizerThreadFactory(""),
                new ThreadPoolExecutor.AbortPolicy()
        );

        class CustomizerThreadFactory implements ThreadFactory {

            private final AtomicInteger poolNumber = new AtomicInteger(1);

            private String threadNamePrefix;

            public CustomizerThreadFactory(String threadNamePrefix) {
                this.threadNamePrefix = threadNamePrefix;
            }

            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, threadNamePrefix + "-pool-" + poolNumber.getAndIncrement());
            }
        }

        //处理器被Acceptor启动后，会唤醒选择器继续查找
        public Handler (Selector sel, SocketChannel c) throws IOException {
            socket = c;
//            this.sel = sel;
            c.configureBlocking(false);
            //Optionally try first read now 这里设置为0是啥意思？
            sk = socket.register(sel, 0);
            sk.attach(this);
            sk.interestOps(SelectionKey.OP_READ);
            sel.wakeup(); //什么时候被阻塞的？ 分发器再selector.select时会阻塞线程
        }

        @Override
        public void run() {
            try {
                if (state == READING) {
                    read();
                } else if (state == SENDING) {
                    send();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //如果单线程时，不需要加锁
        synchronized void read() throws IOException {
            socket.read(input);
            if (inputIsComplete()) {

                //多线程
                state = PROCESSING;
                pool.execute(new Processer(input));

                //单线程处理
//                process(input);
//                state = SENDING;
//                sk.interestOps(SelectionKey.OP_WRITE);
            }
        }
        void send() throws IOException {
            output.clear();
            output.put("服务器已收到，over".getBytes());
            output.flip();
            socket.write(output);
            if (outputIsComplet()) {
                //取消对应的channel和 事件的注册
                sk.cancel();
            }
        }

        boolean inputIsComplete() {return true;}
        boolean outputIsComplet() {return true;}
        void process(ByteBuffer input) {
            //13. 获取当前选择器上“读就绪”状态的通道
            int len = 0;
            while(input.position() > 0 ){
                len = input.limit() - input.position();
                input.flip();
                System.out.println(new String(input.array(), 0, len));
                input.clear();
            }
        }

        //业务处理 脱离主线程
        synchronized void processAndHandOff(ByteBuffer input) {
            process(input);
            state = SENDING;
            sk.interestOps(SelectionKey.OP_WRITE);
//            sel.wakeup(); //什么时候被阻塞的？ 分发器再selector.select时会阻塞线程
        }

        class Processer implements Runnable {

            private ByteBuffer input;

            public Processer(ByteBuffer input) {
                this.input = input;
            }

            @Override
            public void run() {
                processAndHandOff(input);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Reactor reactor = new Reactor(9999);
        Thread server = new Thread(reactor);
        server.start();
        //这里不能中断
        while (!server.isInterrupted()) {
        }
        if (reactor.selector != null) {
            reactor.selector.close();
        }
        if (reactor.serverSocket != null) {
            reactor.serverSocket.close();
        }
    }
}
