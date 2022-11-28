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

public class MultipleReactor extends AbstractReactor implements Runnable {

    final Selector selector;

    final static Selector[] subSelectors = new Selector[2];
    static int next = 0;

    ThreadPoolExecutor subReactorsPool = new ThreadPoolExecutor(
            1,
            1,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingDeque<>(500),
            new CustomizerThreadFactory(""),
            new ThreadPoolExecutor.AbortPolicy()
    );

    final ServerSocketChannel serverSocket;

    //1.setup
    public MultipleReactor(int port) throws IOException {
        selector = Selector.open(); //开启选择器
        subSelectors[0] = Selector.open(); //开启选择器
        subSelectors[1] = Selector.open(); //开启选择器
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(port));
        serverSocket.configureBlocking(false);
        SelectionKey sk = serverSocket.register(selector, SelectionKey.OP_ACCEPT); //main reactor负责监听accept请求
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
        public synchronized void run() {
            try {
                SocketChannel c = serverSocket.accept();
                if (c != null) {
                    accpectQueue.offer(new SubReactor(c, subSelectors[next]));
//                    subReactorsPool.execute(new SubReactor(c, subSelectors[next]));
                }
                if (++next == subSelectors.length) {
                    next = 0;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        for(Selector selector : subSelectors) {
            new Thread(new SubReactor()).start();
        }
        MultipleReactor reactor = new MultipleReactor(9999);
        Thread server = new Thread(reactor);
        server.start();
        //这里不能中断
        while (!server.isInterrupted()) {
        }
        if (reactor.selector != null) {
            reactor.selector.close();
        }
        if (reactor.subSelectors != null) {
            for (Selector selector : reactor.subSelectors) {
                selector.close();
            }
        }
        if (reactor.serverSocket != null) {
            reactor.serverSocket.close();
        }
    }
}

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

class SubReactor extends AbstractReactor implements Runnable {

    private SocketChannel socketChannel;
    private Selector selector;

    public SubReactor(){}

    public SubReactor(SocketChannel c, Selector s) throws IOException {
        socketChannel = c;
        selector = s;
        new Handler(selector, socketChannel);
    }

    @Override
    public void run() {
        try {
            while (!Thread.interrupted()) {
                SubReactor subReactor;
                if ((subReactor = (SubReactor) accpectQueue.poll())!= null) {
                    //Selects a set of keys whose corresponding channels are ready for I/O operations.
                    selector.select();
                    Set<SelectionKey> selected = selector.selectedKeys();
                    Iterator<SelectionKey> it = selected.iterator();
                    while (it.hasNext()) {
                        dispatch(it.next());
                    }
                    selected.clear();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void dispatch (SelectionKey sk) {
        Runnable r = (Runnable) sk.attachment();
        if (r != null) {
            r.run();
        }
    }
}


//4.Handler Setup
final class Handler implements Runnable {

    final int MAXIN = 100;
    final int MAXOUT = 100;

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

class AbstractReactor {
    protected final LinkedBlockingQueue accpectQueue = new LinkedBlockingQueue();
}