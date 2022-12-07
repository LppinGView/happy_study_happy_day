package com.lpp.demo.copyZoo.server;

import com.sun.istack.internal.NotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NIOServerCnxnFactory {

    private int numSelectorThreads = 4;
    private final Set<SelectorThread> selectorThreads = new HashSet<SelectorThread>();

    ServerSocketChannel ss;

    protected ZooKeeperServer zkServer;

    private abstract class AbstractSelectThread extends Thread {

        protected final Selector selector;

        public AbstractSelectThread(String name) throws IOException {
            super(name);
            // Allows the JVM to shutdown even if this thread is still running.
            setDaemon(true);
            this.selector = Selector.open();
        }

        public void wakeupSelector() {
            selector.wakeup();
        }

        protected void closeSelector() {
            try {
                selector.close();
            } catch (IOException e) {
                System.out.println("ignored exception during selector close."+ e);
//                LOG.warn("ignored exception during selector close.", e);
            }
        }

        protected void cleanupSelectionKey(SelectionKey key) {
            if (key != null) {
                try {
                    key.cancel();
                } catch (Exception ex) {
                    System.out.println("ignoring exception during selectionkey cancel" + ex);
//                    LOG.debug("ignoring exception during selectionkey cancel", ex);
                }
            }
        }
    }

    /**
     * 选择器线程，从接收队列中获取channel，注册读写事件
     */
    public class SelectorThread extends AbstractSelectThread {

        private final int id;
        private final Queue<SocketChannel> acceptedQueue;
        private final Queue<SelectionKey> updateQueue;

        final int MAXIN = 100;
        final int MAXOUT = 100;
        ByteBuffer input = ByteBuffer.allocate(MAXIN); //用于channel的读
        ByteBuffer output = ByteBuffer.allocate(MAXOUT);//用于channel的写

        ThreadPoolExecutor pool = new ThreadPoolExecutor(
                3,
                5,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(500),
                new CustomizerThreadFactory(""),
                new ThreadPoolExecutor.AbortPolicy()
        );

        public SelectorThread(int id) throws IOException {
            super("NIOServerCxnFactory.SelectorThread-" + id);
            this.id = id;
            acceptedQueue = new LinkedBlockingQueue<SocketChannel>();
            updateQueue = new LinkedBlockingQueue<SelectionKey>();
        }

        /**
         * Place new accepted connection onto a queue for adding. Do this
         * so only the selector thread modifies what keys are registered
         * with the selector.
         */
        public boolean addAcceptedConnection(SocketChannel accepted) {
            if (!acceptedQueue.offer(accepted)) {
                return false;
            }
            wakeupSelector();
            return true;
        }

        public boolean addInterestOpsUpdateRequest(SelectionKey sk) {
            if (!updateQueue.offer(sk)) {
                return false;
            }
            wakeupSelector();
            return true;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        select();
                        processAcceptedConnections();
                        processInterestOpsUpdateRequests();
                    } catch (RuntimeException e) {
                        System.out.println("Ignoring unexpected runtime exception" + e);
//                        LOG.warn("Ignoring unexpected runtime exception", e);
                    } catch (Exception e) {
                        System.out.println("Ignoring unexpected exception" + e);
//                        LOG.warn("Ignoring unexpected exception", e);
                    }
                }

            } finally {
                closeSelector();
                // This will wake up the accept thread and the other selector
                // threads, and tell the worker thread pool to begin shutdown.
//                NIOServerCnxnFactory.this.stop();
                System.out.println("selector thread exitted run method");
//                LOG.info("selector thread exitted run method");
            }
        }

        private void select() {
            try {
                selector.select();

                Set<SelectionKey> selected = selector.selectedKeys();
                ArrayList<SelectionKey> selectedList = new ArrayList<SelectionKey>(selected);
                Collections.shuffle(selectedList);
                Iterator<SelectionKey> selectedKeys = selectedList.iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selected.remove(key);

                    if (!key.isValid()) {
                        cleanupSelectionKey(key);
                        continue;
                    }
                    if (key.isReadable() || key.isWritable()) {
                        handleIO(key);
                    } else {
                        System.out.println("Unexpected ops in select " + key.readyOps());
//                        LOG.warn("Unexpected ops in select {}", key.readyOps());
                    }
                }
            } catch (IOException e) {
                System.out.println("Ignoring IOException while selecting" + e);
//                LOG.warn("Ignoring IOException while selecting", e);
            }
        }

        private void handleIO(SelectionKey key) {
            IOWorkRequest workRequest = new IOWorkRequest(this, key);
//            NIOServerCnxn cnxn = (NIOServerCnxn) key.attachment();
//
//            // Stop selecting this key while processing on its
//            // connection
//            cnxn.disableSelectable();
//            key.interestOps(0);
//            touchCnxn(cnxn);
//            workerPool.schedule(workRequest);
            // Stop selecting this key while processing on its
            // connection

//            System.out.println("开始处理逻辑");
//            if (key.isReadable()) {
//                read(key);
//            } else if (key.isWritable()) {
//                send(key);
//            }

            key.interestOps(0);
            workerPool.schedule(workRequest);
        }

        private void processAcceptedConnections() {
            SocketChannel accepted;
            while ((accepted = acceptedQueue.poll()) != null) {
                SelectionKey key = null;
                try {
                    key = accepted.register(selector, SelectionKey.OP_READ);
                    NIOServerCnxn cnxn = createConnection(accepted, key, this);
                    key.attach(cnxn);
//                    addCnxn(cnxn);
                } catch (IOException e) {
                    // register, createConnection
                    cleanupSelectionKey(key);
//                    fastCloseSock(accepted);
                }
            }
        }

        /**
         * Iterate over the queue of connections ready to resume selection,
         * and restore their interest ops selection mask.
         */
        private void processInterestOpsUpdateRequests() {
            SelectionKey key;
            while ((key = updateQueue.poll()) != null) {
                if (!key.isValid()) {
                    cleanupSelectionKey(key);
                }
                NIOServerCnxn cnxn = (NIOServerCnxn) key.attachment();
                if (cnxn.isSelectable()) {
                    key.interestOps(cnxn.getInterestOps());
                }
            }
        }

        boolean inputIsComplete() {return true;}
        boolean outputIsComplet() {return true;}

        //如果单线程时，不需要加锁
        synchronized void read(SelectionKey key) {
            try {
                SocketChannel socket = (SocketChannel) key.channel();
                input.clear();
                socket.read(input);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (inputIsComplete()) {

                //多线程
                pool.execute(new Processer(input, key));

                //单线程处理
//                process(input);
//                state = SENDING;
//                sk.interestOps(SelectionKey.OP_WRITE);
            }
        }
        synchronized void send(SelectionKey sk){
            try {
                SocketChannel socket = (SocketChannel) sk.channel();
                output.clear();
                output.put("服务器已收到，over".getBytes());
                output.flip();
                socket.write(output);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (outputIsComplet()) {
                //取消对应的channel和 事件的注册
//                sk.cancel();
            }
        }

        class Processer implements Runnable {

            private ByteBuffer input;
            private final SelectionKey sk;

            public Processer(ByteBuffer input, SelectionKey key) {
                this.input = input;
                this.sk = key;
            }

            @Override
            public void run() {
                processAndHandOff(input);
            }

            //业务处理 脱离主线程
            void processAndHandOff(ByteBuffer input) {
                process(input);
                sk.interestOps(SelectionKey.OP_WRITE);
//            sel.wakeup(); //什么时候被阻塞的？ 分发器再selector.select时会阻塞线程
            }

            synchronized void process(ByteBuffer input) {
                //13. 获取当前选择器上“读就绪”状态的通道
                int len = 0;
                while(input.position() > 0 ){
                    len = input.limit() - input.position();
                    input.flip();
                    System.out.println(new String(input.array(), 0, len));
                    input.clear();
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
    }

    private class IOWorkRequest extends WorkerService.WorkRequest {

        private final SelectorThread selectorThread;
        private final SelectionKey key;
        private final NIOServerCnxn cnxn;

        IOWorkRequest(SelectorThread selectorThread, SelectionKey key) {
            this.selectorThread = selectorThread;
            this.key = key;
            this.cnxn = (NIOServerCnxn) key.attachment();
        }

        @Override
        public void doWork() throws InterruptedException {
            if (!key.isValid()) {
                selectorThread.cleanupSelectionKey(key);
                return;
            }

            if (key.isReadable() || key.isWritable()) {
                cnxn.doIO(key);

                // Check if we shutdown or doIO() closed this connection
//                if (stopped) {
//                    cnxn.close(ServerCnxn.DisconnectReason.SERVER_SHUTDOWN);
//                    return;
//                }
                if (!key.isValid()) {
                    selectorThread.cleanupSelectionKey(key);
                    return;
                }
//                touchCnxn(cnxn);
            }
        }
    }

    /**
     * 获取连接，并为选择器线程循环分配channel，channel放入某个选择器的队列中
     */
    private class AcceptThread extends AbstractSelectThread {

        private final ServerSocketChannel acceptSocket;
        private final SelectionKey acceptKey;
        private final Collection<SelectorThread> selectorThreads;
        private Iterator<SelectorThread> selectorIterator;


        public AcceptThread(ServerSocketChannel ss, InetSocketAddress addr, Set<SelectorThread> selectorThreads) throws IOException {
            super("NIOServerCxnFactory.AcceptThread:" + addr);
            this.acceptSocket = ss;
            this.acceptKey = acceptSocket.register(selector, SelectionKey.OP_ACCEPT);
            this.selectorThreads = Collections.unmodifiableList(new ArrayList<SelectorThread>(selectorThreads));
            selectorIterator = this.selectorThreads.iterator();
        }

        public void run() {
            try {
                while (!acceptSocket.socket().isClosed()) {
                    try {
                        select();
                    } catch (RuntimeException e) {
                        System.out.println("Ignoring unexpected runtime exception" + e);
//                        LOG.warn("Ignoring unexpected runtime exception", e);
                    } catch (Exception e) {
                        System.out.println("Ignoring unexpected exception" + e);
//                        LOG.warn("Ignoring unexpected exception", e);
                    }
                }
            } finally {
                closeSelector();
                // This will wake up the selector threads, and tell the
                // worker thread pool to begin shutdown.
//                if (!reconfiguring) {
//                    NIOServerCnxnFactory.this.stop();
//                }
                System.out.println("accept thread exitted run method");
//                LOG.info("accept thread exitted run method");
            }
        }

        private void select() {
            try {
                selector.select();

                Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }
                    if (key.isAcceptable()) {
                        if (!doAccept()) {
                            // If unable to pull a new connection off the accept
                            // queue, pause accepting to give us time to free
                            // up file descriptors and so the accept thread
                            // doesn't spin in a tight loop.
//                            pauseAccept(10);
                        }
                    } else {
                        System.out.println("Unexpected ops in accept select " + key.readyOps());
//                        LOG.warn("Unexpected ops in accept select {}", key.readyOps());
                    }
                }
            } catch (IOException e) {
                System.out.println("Ignoring IOException while selecting" + e);
//                LOG.warn("Ignoring IOException while selecting", e);
            }
        }

        /**
         * 接收一个新连接，将该连接循环分配到给选择器线程，返回是否从接收队列拉取到连接
         * @return
         */
        private boolean doAccept() {
            boolean accepted = false;
            SocketChannel sc = null;

            try {
                sc = acceptSocket.accept();
                accepted = true;
                System.out.println("Accepted socket connection from " + sc.socket().getRemoteSocketAddress());

                sc.configureBlocking(false);

                // Round-robin assign this connection to a selector thread
                if (!selectorIterator.hasNext()) {
                    selectorIterator = selectorThreads.iterator();
                }
                SelectorThread selectorThread = selectorIterator.next();
                if (!selectorThread.addAcceptedConnection(sc)) {
                    throw new IOException("Unable to add connection to selector queue");
                }

            } catch (IOException e) {
                System.out.println("Error accepting new connection: " + e.getMessage());
            }
            return accepted;
        }
    }

    protected NIOServerCnxn createConnection(SocketChannel sock, SelectionKey sk, SelectorThread selectorThread) throws IOException {
        return new NIOServerCnxn(zkServer, sock, sk, this, selectorThread);
    }

    public NIOServerCnxnFactory() {
    }
    private AcceptThread acceptThread;

    public void configure() throws IOException {
        for (int i = 0; i < numSelectorThreads; ++i) {
            selectorThreads.add(new SelectorThread(i));
        }

        this.ss = ServerSocketChannel.open();
        ss.socket().bind(new InetSocketAddress(9999));
        ss.configureBlocking(false);

        acceptThread = new AcceptThread(ss, new InetSocketAddress(9999), selectorThreads);
    }

    protected WorkerService workerPool;
    private int numWorkerThreads = 64;

    public void start() {
        if (workerPool == null) {
            workerPool = new WorkerService("NIOWorker", numWorkerThreads, false);
        }
        for (SelectorThread thread : selectorThreads) {
            if (thread.getState() == Thread.State.NEW) {
                thread.start();
            }
        }
        // ensure thread is started once and only once
        if (acceptThread.getState() == Thread.State.NEW) {
            acceptThread.start();
        }
//        if (expirerThread.getState() == Thread.State.NEW) {
//            expirerThread.start();
//        }
    }
}

class Test {
    public static void main(String[] args) throws IOException, InterruptedException {
        final CountDownLatch shutdownLatch = new CountDownLatch(1);
        NIOServerCnxnFactory factory = new NIOServerCnxnFactory();
        factory.configure();
        factory.start();
        System.out.println("server start...");
        shutdownLatch.await();
    }
}