package com.lpp.demo.copyZoo.server.util;

import com.lpp.demo.copyZoo.jute.BinaryInputArchive;
import com.lpp.demo.copyZoo.jute.InputArchive;
import com.lpp.demo.copyZoo.jute.OutputArchive;
import com.lpp.demo.copyZoo.jute.Record;
import com.lpp.demo.copyZoo.server.DataTree;
import com.lpp.demo.copyZoo.server.TxnLogEntry;
import com.lpp.demo.copyZoo.server.ZooDefs.*;
import com.lpp.demo.copyZoo.server.ZooKeeperServer;
import com.lpp.demo.copyZoo.zookeeper.txn.CreateSessionTxn;
import com.lpp.demo.copyZoo.zookeeper.txn.CreateTxn;
import com.lpp.demo.copyZoo.zookeeper.txn.TxnDigest;
import com.lpp.demo.copyZoo.zookeeper.txn.TxnHeader;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SerializeUtils {

    public static TxnLogEntry deserializeTxn(byte[] txnBytes) throws IOException {
        TxnHeader hdr = new TxnHeader();
        final ByteArrayInputStream bais = new ByteArrayInputStream(txnBytes);
        InputArchive ia = BinaryInputArchive.getArchive(bais);

        hdr.deserialize(ia, "hdr");
        bais.mark(bais.available());
        Record txn = null;
        switch (hdr.getType()) {
            case OpCode.createSession:
                // This isn't really an error txn; it just has the same
                // format. The error represents the timeout
                txn = new CreateSessionTxn();
                break;
            case OpCode.closeSession:
//                txn = ZooKeeperServer.isCloseSessionTxnEnabled()
//                        ?  new CloseSessionTxn() : null;
                break;
            case OpCode.create:
            case OpCode.create2:
                txn = new CreateTxn();
                break;
            case OpCode.createTTL:
//                txn = new CreateTTLTxn();
                break;
//            case OpCode.createContainer:
//                txn = new CreateContainerTxn();
//                break;
//            case OpCode.delete:
//            case OpCode.deleteContainer:
//                txn = new DeleteTxn();
//                break;
//            case OpCode.reconfig:
//            case OpCode.setData:
//                txn = new SetDataTxn();
//                break;
//            case OpCode.setACL:
//                txn = new SetACLTxn();
//                break;
//            case OpCode.error:
//                txn = new ErrorTxn();
//                break;
//            case OpCode.multi:
//                txn = new MultiTxn();
//                break;
//            default:
//                throw new IOException("Unsupported Txn with type=" + hdr.getType());
        }
        if (txn != null) {
            try {
                txn.deserialize(ia, "txn");
            } catch (EOFException e) {
                // perhaps this is a V0 Create
                if (hdr.getType() == OpCode.create) {
                    CreateTxn create = (CreateTxn) txn;
                    bais.reset();
//                    CreateTxnV0 createv0 = new CreateTxnV0();
//                    createv0.deserialize(ia, "txn");
                    // cool now make it V1. a -1 parentCVersion will
                    // trigger fixup processing in processTxn
//                    create.setPath(createv0.getPath());
//                    create.setData(createv0.getData());
//                    create.setAcl(createv0.getAcl());
//                    create.setEphemeral(createv0.getEphemeral());
                    create.setParentCVersion(-1);
                } else if (hdr.getType() == OpCode.closeSession) {
                    // perhaps this is before CloseSessionTxn was added,
                    // ignore it and reset txn to null
                    txn = null;
                } else {
                    throw e;
                }
            }
        }
        TxnDigest digest = null;

//        if (ZooKeeperServer.isDigestEnabled()) {
//            digest = new TxnDigest();
//            try {
//                digest.deserialize(ia, "digest");
//            } catch (EOFException exception) {
//                // may not have digest in the txn
//                digest = null;
//            }
//        }

        return new TxnLogEntry(txn, hdr, digest);
    }

    public static void serializeSnapshot(DataTree dt, OutputArchive oa, Map<Long, Integer> sessions) throws IOException {
        HashMap<Long, Integer> sessSnap = new HashMap<Long, Integer>(sessions);
        oa.writeInt(sessSnap.size(), "count");
        for (Map.Entry<Long, Integer> entry : sessSnap.entrySet()) {
            oa.writeLong(entry.getKey().longValue(), "id");
            oa.writeInt(entry.getValue().intValue(), "timeout");
        }
        dt.serialize(oa, "tree");
    }

    public static void deserializeSnapshot(DataTree dt, InputArchive ia, Map<Long, Integer> sessions) throws IOException {
        int count = ia.readInt("count");
        while (count > 0) {
            long id = ia.readLong("id");
            int to = ia.readInt("timeout");
            sessions.put(id, to);
//            if (LOG.isTraceEnabled()) {
//                ZooTrace.logTraceMessage(
//                        LOG,
//                        ZooTrace.SESSION_TRACE_MASK,
//                        "loadData --- session in archive: " + id + " with timeout: " + to);
//            }
            count--;
        }
        dt.deserialize(ia, "tree");
    }
}
