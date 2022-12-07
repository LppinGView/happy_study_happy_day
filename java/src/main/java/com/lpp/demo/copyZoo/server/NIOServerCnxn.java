package com.lpp.demo.copyZoo.server;

import com.lpp.demo.copyZoo.jute.BinaryInputArchive;
import com.lpp.demo.copyZoo.jute.BinaryOutputArchive;
import com.lpp.demo.copyZoo.jute.Record;
import com.lpp.demo.copyZoo.zookeeper.compat.ProtocolManager;
import com.lpp.demo.copyZoo.zookeeper.data.Stat;
import com.lpp.demo.copyZoo.zookeeper.proto.ConnectRequest;
import com.lpp.demo.copyZoo.server.NIOServerCnxnFactory.SelectorThread;
import com.lpp.demo.copyZoo.zookeeper.proto.ReplyHeader;
import com.lpp.demo.copyZoo.zookeeper.proto.RequestHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 处理客户端IO事务，每个客户端对应一个，但是只有一个线程处理
 */
public class NIOServerCnxn {

    private final NIOServerCnxnFactory factory;

    private final SocketChannel sock;

    private final ByteBuffer lenBuffer = ByteBuffer.allocate(4);

    protected ByteBuffer incomingBuffer = lenBuffer;

    private final Queue<ByteBuffer> outgoingBuffers = new LinkedBlockingQueue<ByteBuffer>();

    private final SelectorThread selectorThread;

    private final SelectionKey sk;

    private boolean initialized;

    final ZooKeeperServer zkServer = new ZooKeeperServer();

    public final ProtocolManager protocolManager = new ProtocolManager();

    public NIOServerCnxn(ZooKeeperServer zk, SocketChannel sock, SelectionKey sk, NIOServerCnxnFactory factory, SelectorThread selectorThread) throws IOException {
//        super(zk);
        this.sock = sock;
        this.sk = sk;
        this.factory = factory;
        this.selectorThread = selectorThread;
//        if (this.factory.login != null) {
//            this.zooKeeperSaslServer = new ZooKeeperSaslServer(factory.login);
//        }
        sock.socket().setTcpNoDelay(true);
        /* set socket linger to false, so that socket close does not block */
        sock.socket().setSoLinger(false, -1);
//        sock.socket().setKeepAlive(clientTcpKeepAlive);
        InetAddress addr = ((InetSocketAddress) sock.socket().getRemoteSocketAddress()).getAddress();
//        addAuthInfo(new Id("ip", addr.getHostAddress()));
//        this.sessionTimeout = factory.sessionlessCnxnTimeout;
    }

    // returns whether we are interested in writing, which is determined
    // by whether we have any pending buffers on the output queue or not
    private boolean getWriteInterest() {
        return !outgoingBuffers.isEmpty();
    }

    // returns whether we are interested in taking new requests, which is
    // determined by whether we are currently throttled or not
    private boolean getReadInterest() {
        return !throttled.get();
    }

    private final AtomicBoolean throttled = new AtomicBoolean(false);

    public int getInterestOps() {
        if (!isSelectable()) {
            return 0;
        }
        int interestOps = 0;
        if (getReadInterest()) {
            interestOps |= SelectionKey.OP_READ;
        }
        if (getWriteInterest()) {
            interestOps |= SelectionKey.OP_WRITE;
        }
        return interestOps;
    }

    /**
     * Handles read/write IO on connection.
     */
    void doIO(SelectionKey k) {
        try {
            if (k.isReadable()) {

                int rc = sock.read(incomingBuffer);
                //todo 处理失败读

                //incomingBuffer 已满
                if (incomingBuffer.remaining() == 0) {
                    boolean isPayload;
                    if (incomingBuffer == lenBuffer) { // start of next request
                        incomingBuffer.flip();
                        isPayload = readLength(k);
                        incomingBuffer.clear();
                    } else {
                        // continuation
                        isPayload = true;
                    }
                    if (isPayload) { // not the case for 4letterword
                        readPayload();
                    } else {
                        // four letter words take care
                        // need not do anything else
                        return;
                    }
                }

//                read(k);
            } else if (k.isWritable()) {
                handleWrite(k);
//                send(k);
            }
        } catch (IOException e) {
//            LOG.warn("Close of session 0x{}", Long.toHexString(sessionId), e);
//            close(DisconnectReason.IO_EXCEPTION);
        }
    }

    /** Reads the first 4 bytes of lenBuffer, which could be true length or
     *  four letter word.
     *
     * @param k
     * @return
     * @throws IOException
     */
    private boolean readLength(SelectionKey k) throws IOException {
        // Read the length, now get the buffer
        int len = lenBuffer.getInt();

        //todo 前面4字节校验，有问题直接中止

        //没问题，分配指定大小的缓冲区
        incomingBuffer = ByteBuffer.allocate(len);
        return true;
    }

    private void readPayload() throws IOException {
        if (incomingBuffer.remaining() != 0) { // have we read length bytes?
            int rc = sock.read(incomingBuffer); // sock is non-blocking, so ok
            if (rc < 0) {
                //todo 处理失败读
            }
        }

        if (incomingBuffer.remaining() == 0) { // have we read length bytes?
            incomingBuffer.flip();

            //todo 统计已收到多少packet
//            packetReceived(4 + incomingBuffer.remaining());

            if (!initialized) {
                readConnectRequest();
            } else {
                readRequest();
            }
            lenBuffer.clear();
            incomingBuffer = lenBuffer;
        }
    }

    /**
     * 处理链接请求, 配置session
     * @throws IOException
     */
    private void readConnectRequest() throws IOException{
//        if (!isZKServerRunning()) {
//            throw new IOException("ZooKeeperServer not running");
//        }
        BinaryInputArchive bia = BinaryInputArchive.getArchive(new ByteBufferInputStream(incomingBuffer));
        ConnectRequest request = protocolManager.deserializeConnectRequest(bia);
        zkServer.processConnectRequest(this, request);
        initialized = true;
    }

    protected void readRequest() throws IOException {
        //解析请求头
        RequestHeader h = new RequestHeader();
        ByteBufferInputStream.byteBuffer2Record(incomingBuffer, h);
        //解析请求数据
        RequestRecord request = RequestRecord.fromBytes(incomingBuffer.slice());
        zkServer.processPacket(this, h, request);
    }


    void handleWrite(SelectionKey k) throws IOException {
        try {
            SocketChannel socket = (SocketChannel) sk.channel();
//            output.clear();
//            output.put("服务器已收到，over".getBytes());
//            output.flip();
            socket.write(outgoingBuffers.peek());
        } catch (IOException e) {
            e.printStackTrace();
        }
//        if (outputIsComplet()) {
//            //取消对应的channel和 事件的注册
////                sk.cancel();
//        }
    }

    private static final ByteBuffer packetSentinel = ByteBuffer.allocate(0);

    public int sendResponse(ReplyHeader h, Record r, String tag) throws IOException {
        return sendResponse(h, r, tag, null, null, -1);
    }

    public int sendResponse(ReplyHeader h, Record r, String tag, String cacheKey, Stat stat, int opCode) {
        int responseSize = 0;
        try {
            ByteBuffer[] bb = serialize(h, r, tag, cacheKey, stat, opCode);
            responseSize = bb[0].getInt();
            bb[0].rewind();
            sendBuffer(bb);
//            decrOutstandingAndCheckThrottle(h);
        } catch (Exception e) {
//            LOG.warn("Unexpected exception. Destruction averted.", e);
        }
        return responseSize;
    }

    protected ByteBuffer[] serialize(ReplyHeader h, Record r, String tag,
                                     String cacheKey, Stat stat, int opCode) throws IOException {
        byte[] header = serializeRecord(h);
        byte[] data = null;
        if (r != null) {
            ResponseCache cache = null;
//            Counter cacheHit = null, cacheMiss = null;
            switch (opCode) {
//                case OpCode.getData : {
//                    cache = zkServer.getReadResponseCache();
//                    cacheHit = ServerMetrics.getMetrics().RESPONSE_PACKET_CACHE_HITS;
//                    cacheMiss = ServerMetrics.getMetrics().RESPONSE_PACKET_CACHE_MISSING;
//                    break;
//                }
//                case OpCode.getChildren2 : {
//                    cache = zkServer.getGetChildrenResponseCache();
//                    cacheHit = ServerMetrics.getMetrics().RESPONSE_PACKET_GET_CHILDREN_CACHE_HITS;
//                    cacheMiss = ServerMetrics.getMetrics().RESPONSE_PACKET_GET_CHILDREN_CACHE_MISSING;
//                    break;
//                }
                default:
                    // op codes where response cache is not supported.
            }

//            if (cache != null && stat != null && cacheKey != null && !cacheKey.endsWith(Quotas.statNode)) {
                // Use cache to get serialized data.
                //
                // NB: Tag is ignored both during cache lookup and serialization,
                // since is is not used in read responses, which are being cached.
                data = cache.get(cacheKey, stat);
                if (data == null) {
                    // Cache miss, serialize the response and put it in cache.
                    data = serializeRecord(r);
                    cache.put(cacheKey, data, stat);
//                    cacheMiss.add(1);
                } else {
//                    cacheHit.add(1);
                }
            } else {
                data = serializeRecord(r);
            }
//        }
        int dataLength = data == null ? 0 : data.length;
        int packetLength = header.length + dataLength;
//        ServerStats serverStats = serverStats();
//        if (serverStats != null) {
//            serverStats.updateClientResponseSize(packetLength);
//        }
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4).putInt(packetLength);
        lengthBuffer.rewind();

        int bufferLen = data != null ? 3 : 2;
        ByteBuffer[] buffers = new ByteBuffer[bufferLen];

        buffers[0] = lengthBuffer;
        buffers[1] = ByteBuffer.wrap(header);
        if (data != null) {
            buffers[2] = ByteBuffer.wrap(data);
        }
        return buffers;
    }

    protected byte[] serializeRecord(Record record) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(ZooKeeperServer.intBufferStartingSizeBytes);
        BinaryOutputArchive bos = BinaryOutputArchive.getArchive(baos);
        bos.writeRecord(record, null);
        return baos.toByteArray();
    }

    public void sendBuffer(ByteBuffer... buffers) {
//        if (LOG.isTraceEnabled()) {
//            LOG.trace("Add a buffer to outgoingBuffers, sk {} is valid: {}", sk, sk.isValid());
//        }

        synchronized (outgoingBuffers) {
            for (ByteBuffer buffer : buffers) {
                outgoingBuffers.add(buffer);
            }
            outgoingBuffers.add(packetSentinel);
        }
        requestInterestOpsUpdate();
    }

    private final AtomicBoolean selectable = new AtomicBoolean(true);

    public boolean isSelectable() {
        return sk.isValid() && selectable.get();
    }

    private void requestInterestOpsUpdate() {
        if (isSelectable()) {
            selectorThread.addInterestOpsUpdateRequest(sk);
        }
    }
}
