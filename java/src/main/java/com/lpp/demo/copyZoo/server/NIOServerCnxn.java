package com.lpp.demo.copyZoo.server;

import com.lpp.demo.copyZoo.jute.BinaryInputArchive;
import com.lpp.demo.copyZoo.zookeeper.compat.ProtocolManager;
import com.lpp.demo.copyZoo.zookeeper.proto.ConnectRequest;
import com.lpp.demo.copyZoo.server.NIOServerCnxnFactory.SelectorThread;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 处理客户端IO事务，每个客户端对应一个，但是只有一个线程处理
 */
public class NIOServerCnxn {

    private final NIOServerCnxnFactory factory;

    private final SocketChannel sock;

    private final ByteBuffer lenBuffer = ByteBuffer.allocate(4);

    protected ByteBuffer incomingBuffer = lenBuffer;

    private final SelectorThread selectorThread;

    private final SelectionKey sk;

    private boolean initialized;

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
        if (!isZKServerRunning()) {
            throw new IOException("ZooKeeperServer not running");
        }
        BinaryInputArchive bia = BinaryInputArchive.getArchive(new ByteBufferInputStream(incomingBuffer));
        ConnectRequest request = protocolManager.deserializeConnectRequest(bia);
//        zkServer.processConnectRequest(this, request);
//        initialized = true;
    }

    protected void readRequest() throws IOException {
//        RequestHeader h = new RequestHeader();
//        ByteBufferInputStream.byteBuffer2Record(incomingBuffer, h);
//        RequestRecord request = RequestRecord.fromBytes(incomingBuffer.slice());
//        zkServer.processPacket(this, h, request);
    }


    void handleWrite(SelectionKey k) throws IOException {}
}
