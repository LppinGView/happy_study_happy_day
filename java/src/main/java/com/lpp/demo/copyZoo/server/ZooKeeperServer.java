package com.lpp.demo.copyZoo.server;

import com.lpp.demo.copyZoo.zookeeper.proto.ConnectRequest;
import com.lpp.demo.copyZoo.zookeeper.proto.ReplyHeader;
import com.lpp.demo.copyZoo.zookeeper.proto.RequestHeader;

import java.io.IOException;

/**
 * 一个服务器一个实例,用于处理各个连接的 客户端请求
 *
 * This class implements a simple standalone ZooKeeperServer. It sets up the
 * following chain of RequestProcessors to process requests:
 * PrepRequestProcessor -&gt; SyncRequestProcessor -&gt; FinalRequestProcessor
 */
public class ZooKeeperServer {

    public static final String INT_BUFFER_STARTING_SIZE_BYTES = "zookeeper.intBufferStartingSizeBytes";
    public static final int DEFAULT_STARTING_BUFFER_SIZE = 1024;
    public static final int intBufferStartingSizeBytes;

    static {
//        long configuredFlushDelay = Long.getLong(FLUSH_DELAY, 0);
//        setFlushDelay(configuredFlushDelay);
//        setMaxWriteQueuePollTime(Long.getLong(MAX_WRITE_QUEUE_POLL_SIZE, configuredFlushDelay / 3));
//        setMaxBatchSize(Integer.getInteger(MAX_BATCH_SIZE, 1000));

        intBufferStartingSizeBytes = Integer.getInteger(INT_BUFFER_STARTING_SIZE_BYTES, DEFAULT_STARTING_BUFFER_SIZE);

//        if (intBufferStartingSizeBytes < 32) {
//            String msg = "Buffer starting size (" + intBufferStartingSizeBytes + ") must be greater than or equal to 32. "
//                    + "Configure with \"-Dzookeeper.intBufferStartingSizeBytes=<size>\" ";
//            LOG.error(msg);
//            throw new IllegalArgumentException(msg);
//        }
//
//        LOG.info("{} = {}", INT_BUFFER_STARTING_SIZE_BYTES, intBufferStartingSizeBytes);
    }


    public void processConnectRequest(NIOServerCnxn cnxn, ConnectRequest request) throws IOException {
        //创建session, 创建Request
        //或者重新打开session

        //往队列中 插入数据 submittedRequests 之后一个线程 开始从队列中获取数据 进行链式 处理请求

    }

    public void processPacket(NIOServerCnxn cnxn, RequestHeader h, RequestRecord request) throws IOException {
        ReplyHeader rh = new ReplyHeader(h.getXid(), 0, 99);
        //直接像 客户端应答请求
        cnxn.sendResponse(rh, null , null);

        //todo 提交请求 本地落库
    }

}
