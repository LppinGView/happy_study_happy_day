package com.lpp.demo.copyZoo.server;

import com.lpp.demo.copyZoo.zookeeper.proto.ConnectRequest;

import java.io.IOException;

/**
 * 一个服务器一个实例,用于处理各个连接的 客户端请求
 *
 * This class implements a simple standalone ZooKeeperServer. It sets up the
 * following chain of RequestProcessors to process requests:
 * PrepRequestProcessor -&gt; SyncRequestProcessor -&gt; FinalRequestProcessor
 */
public class ZooKeeperServer {

    public void processConnectRequest(NIOServerCnxn cnxn, ConnectRequest request) throws IOException {

    }

}
