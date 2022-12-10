package com.lpp.demo.copyZoo.server.persistence;

import com.lpp.demo.copyZoo.zookeeper.txn.CreateTxn;
import com.lpp.demo.copyZoo.zookeeper.txn.TxnHeader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class FileTxnSnapLogTest {

    @Test
    public void append() {
        File file = new File("C:\\Users\\27801\\workplace\\zk\\myDataDir\\version-2");
        TxnLog txnLog = new FileTxnLog(file);
        long currentTimeMillis = System.currentTimeMillis();
        TxnHeader header = new TxnHeader(1L, 1, 3, currentTimeMillis, 1);
        byte[] data = new byte[]{11, 11};
        CreateTxn txn = new CreateTxn("/lpp", data, null ,false, 0);
        try {
            txnLog.append(header,  txn);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
