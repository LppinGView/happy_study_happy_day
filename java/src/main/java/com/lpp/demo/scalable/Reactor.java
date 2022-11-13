package com.lpp.demo.scalable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

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
            while (!Thread.interrupted()) {
                //Selects a set of keys whose corresponding channels are ready for I/O operations.
                selector.select();
                Set<SelectionKey> selected = selector.selectedKeys();
                Iterator<SelectionKey> it = selected.iterator();
                while (it.hasNext()) {
                    dispatch(it.next());
                }
                selected.clear();
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
        ByteBuffer input = ByteBuffer.allocate(MAXIN); //用于channel的读
        ByteBuffer output = ByteBuffer.allocate(MAXOUT);//用于channel的写
        static final int READING = 0, SENDING = 1;
        int state = READING;

        //处理器被Acceptor启动后，会唤醒选择器继续查找
        public Handler (Selector sel, SocketChannel c) throws IOException {
            socket = c;
            c.configureBlocking(false);
            //Optionally try first read now 这里设置为0是啥意思？
            sk = socket.register(sel, 0);
            sk.attach(this);
            sk.interestOps(SelectionKey.OP_ACCEPT);
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

        void read() throws IOException {
            socket.read(input);
            if (inputIsComplete()) {
                process();
                state = SENDING;
                sk.interestOps(SelectionKey.OP_WRITE);
            }
        }
        void send() throws IOException {
            socket.write(output);
            if (outputIsComplet()) {
                //取消对应的channel和 事件的注册
                sk.cancel();
            }
        }

        boolean inputIsComplete() {return true;}
        boolean outputIsComplet() {return true;}
        void process() {}
    }
}
