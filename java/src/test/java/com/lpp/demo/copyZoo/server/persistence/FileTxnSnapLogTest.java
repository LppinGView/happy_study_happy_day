package com.lpp.demo.copyZoo.server.persistence;

import com.lpp.demo.copyZoo.zookeeper.txn.CreateTxn;
import com.lpp.demo.copyZoo.zookeeper.txn.TxnHeader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class FileTxnSnapLogTest {

    @Test
    public void append() throws IOException {
        File file = new File("C:\\Users\\27801\\workplace\\zk\\myDataDir\\version-2");
        TxnLog txnLog = new FileTxnLog(file);
        long currentTimeMillis = System.currentTimeMillis();
        TxnHeader header = new TxnHeader(2342L, 11,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          12, currentTimeMillis, 1);
        byte[] data = "fsasfs".getBytes();
        CreateTxn txn = new CreateTxn("/", data, null ,false, 2);
        try {
            txnLog.append(header,  txn);
            txnLog.commit();
        } catch (IOException e) {
            txnLog.close();
            e.printStackTrace();
        }
    }
}