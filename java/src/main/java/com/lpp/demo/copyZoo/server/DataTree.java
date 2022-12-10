/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lpp.demo.copyZoo.server;

import com.lpp.demo.copyZoo.Quotas;
import com.lpp.demo.copyZoo.jute.InputArchive;
import com.lpp.demo.copyZoo.jute.OutputArchive;
import com.lpp.demo.copyZoo.jute.Record;
import com.lpp.demo.copyZoo.zookeeper.data.ACL;
import com.lpp.demo.copyZoo.zookeeper.data.Stat;
import com.lpp.demo.copyZoo.zookeeper.data.StatPersisted;
import com.lpp.demo.copyZoo.zookeeper.txn.CreateTxn;
import com.lpp.demo.copyZoo.zookeeper.txn.TxnDigest;
import com.lpp.demo.copyZoo.zookeeper.txn.TxnHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class maintains the tree data structure. It doesn't have any networking
 * or client connection code in it so that it can be tested in a stand alone
 * way.
 * <p>
 * The tree maintains two parallel data structures: a hashtable that maps from
 * full paths to DataNodes and a tree of DataNodes. All accesses to a path is
 * through the hashtable. The tree is traversed only when serializing to disk.
 */
public class DataTree {

    private final NodeHashMap nodes;

    /** cached total size of paths and data for all DataNodes */
    private final AtomicLong nodeDataSize = new AtomicLong(0);

    /** the root of zookeeper tree */
    private static final String rootZookeeper = "/";

    /** the zookeeper nodes that acts as the management and status node **/
    private static final String procZookeeper = Quotas.procZookeeper;

    /** this will be the string thats stored as a child of root */
    private static final String procChildZookeeper = procZookeeper.substring(1);
    /**
     * the zookeeper quota node that acts as the quota management node for
     * zookeeper
     */
    private static final String quotaZookeeper = Quotas.quotaZookeeper;

    /** this will be the string thats stored as a child of /zookeeper */
    private static final String quotaChildZookeeper = quotaZookeeper.substring(procZookeeper.length() + 1);

    private DataNode root = new DataNode(new byte[0], -1L, new StatPersisted());
    /**
     * create a /zookeeper filesystem that is the proc filesystem of zookeeper
     */
    private final DataNode procDataNode = new DataNode(new byte[0], -1L, new StatPersisted());
    /**
     * create a /zookeeper/quota node for maintaining quota properties for
     * zookeeper
     */
    private final DataNode quotaDataNode = new DataNode(new byte[0], -1L, new StatPersisted());

    public volatile long lastProcessedZxid = 0;

    DataTree(DigestCalculator digestCalculator) {
//        this.digestCalculator = digestCalculator;
        nodes = new NodeHashMapImpl(digestCalculator);

        /* Rather than fight it, let root have an alias */
        nodes.put("", root);
        nodes.putWithoutDigest(rootZookeeper, root);

        /** add the proc node and quota node */
        root.addChild(procChildZookeeper);
        nodes.put(procZookeeper, procDataNode);

        procDataNode.addChild(quotaChildZookeeper);
        nodes.put(quotaZookeeper, quotaDataNode);

//        addConfigNode();

        nodeDataSize.set(approximateDataSize());
        try {
//            dataWatches = WatchManagerFactory.createWatchManager();
//            childWatches = WatchManagerFactory.createWatchManager();
        } catch (Exception e) {
//            LOG.error("Unexpected exception when creating WatchManager, exiting abnormally", e);
//            ServiceUtils.requestSystemExit(ExitCode.UNEXPECTED_ERROR.getValue());
        }
    }

    public void serialize(OutputArchive oa, String tag) throws IOException {
//        serializeAcls(oa);
        serializeNodes(oa);
    }

    public void serializeNodes(OutputArchive oa) throws IOException {
        serializeNode(oa, new StringBuilder());
        // / marks end of stream
        // we need to check if clear had been called in between the snapshot.
        if (root != null) {
            oa.writeString("/", "path");
        }
    }

    void serializeNode(OutputArchive oa, StringBuilder path) throws IOException {
        String pathString = path.toString();
        DataNode node = getNode(pathString);
        if (node == null) {
            return;
        }
        String[] children = null;
        DataNode nodeCopy;
        synchronized (node) {
            StatPersisted statCopy = new StatPersisted();
            copyStatPersisted(node.stat, statCopy);
            //we do not need to make a copy of node.data because the contents
            //are never changed
            nodeCopy = new DataNode(node.data, node.acl, statCopy);
            Set<String> childs = node.getChildren();
            children = childs.toArray(new String[childs.size()]);
        }
        serializeNodeData(oa, pathString, nodeCopy);
        path.append('/');
        int off = path.length();
        for (String child : children) {
            // since this is single buffer being resused
            // we need
            // to truncate the previous bytes of string.
            path.delete(off, Integer.MAX_VALUE);
            path.append(child);
            serializeNode(oa, path);
        }
    }

    // visiable for test
    public void serializeNodeData(OutputArchive oa, String path, DataNode node) throws IOException {
        oa.writeString(path, "path");
        oa.writeRecord(node, "node");
    }

    public DataNode getNode(String path) {
        return nodes.get(path);
    }

    public void deserialize(InputArchive ia, String tag) throws IOException {
//        aclCache.deserialize(ia);
        nodes.clear();
//        pTrie.clear();
        nodeDataSize.set(0);
        String path = ia.readString("path");
        while (!"/".equals(path)) {
            DataNode node = new DataNode();
            ia.readRecord(node, "node");
            nodes.put(path, node);
            synchronized (node) {
//                aclCache.addUsage(node.acl);
            }
            int lastSlash = path.lastIndexOf('/');
            if (lastSlash == -1) {
                root = node;
            } else {
                String parentPath = path.substring(0, lastSlash);
                DataNode parent = nodes.get(parentPath);
                if (parent == null) {
                    throw new IOException("Invalid Datatree, unable to find "
                            + "parent "
                            + parentPath
                            + " of path "
                            + path);
                }
                parent.addChild(path.substring(lastSlash + 1));
                long eowner = node.stat.getEphemeralOwner();
//                EphemeralType ephemeralType = EphemeralType.get(eowner);
//                if (ephemeralType == EphemeralType.CONTAINER) {
//                    containers.add(path);
//                } else if (ephemeralType == EphemeralType.TTL) {
//                    ttls.add(path);
//                } else if (eowner != 0) {
//                    HashSet<String> list = ephemerals.get(eowner);
//                    if (list == null) {
//                        list = new HashSet<String>();
//                        ephemerals.put(eowner, list);
//                    }
//                    list.add(path);
//                }
            }
            path = ia.readString("path");
        }
        // have counted digest for root node with "", ignore here to avoid
        // counting twice for root node
        nodes.putWithoutDigest("/", root);

        nodeDataSize.set(approximateDataSize());

        // we are done with deserializing the
        // the datatree
        // update the quotas - create path trie
        // and also update the stat nodes
//        setupQuota();

//        aclCache.purgeUnused();
    }

    public boolean serializeZxidDigest(OutputArchive oa) throws IOException {
//        if (!ZooKeeperServer.isDigestEnabled()) {
//            return false;
//        }
//
//        ZxidDigest zxidDigest = lastProcessedZxidDigest;
//        if (zxidDigest == null) {
//            // write an empty digest
//            zxidDigest = new ZxidDigest();
//        }
//        zxidDigest.serialize(oa);
        return true;
    }

    public boolean compareDigest(TxnHeader header, Record txn, TxnDigest digest) {
        return true;
    }

    public static class ProcessTxnResult {

        public long clientId;

        public int cxid;

        public long zxid;

        public int err;

        public int type;

        public String path;

        public Stat stat;

        public List<ProcessTxnResult> multiResult;

        /**
         * Equality is defined as the clientId and the cxid being the same. This
         * allows us to use hash tables to track completion of transactions.
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof ProcessTxnResult) {
                ProcessTxnResult other = (ProcessTxnResult) o;
                return other.clientId == clientId && other.cxid == cxid;
            }
            return false;
        }

        /**
         * See equals() to find the rational for how this hashcode is generated.
         *
         * @see ProcessTxnResult#equals(Object)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return (int) ((clientId ^ cxid) % Integer.MAX_VALUE);
        }

    }

    public ProcessTxnResult processTxn(TxnHeader header, Record txn) {
        return this.processTxn(header, txn, false);
    }

    public ProcessTxnResult processTxn(TxnHeader header, Record txn, boolean isSubTxn) {
        ProcessTxnResult rc = new ProcessTxnResult();

        try {
            rc.clientId = header.getClientId();
            rc.cxid = header.getCxid();
            rc.zxid = header.getZxid();
            rc.type = header.getType();
            rc.err = 0;
            rc.multiResult = null;
            switch (header.getType()) {
                case ZooDefs.OpCode.create:
                    CreateTxn createTxn = (CreateTxn) txn;
                    rc.path = createTxn.getPath();
                    createNode(
                            createTxn.getPath(),
                            createTxn.getData(),
                            createTxn.getAcl(),
                            createTxn.getEphemeral() ? header.getClientId() : 0,
                            createTxn.getParentCVersion(),
                            header.getZxid(),
                            header.getTime(),
                            null);
                    break;
                case ZooDefs.OpCode.create2:
                    CreateTxn create2Txn = (CreateTxn) txn;
                    rc.path = create2Txn.getPath();
                    Stat stat = new Stat();
                    createNode(
                            create2Txn.getPath(),
                            create2Txn.getData(),
                            create2Txn.getAcl(),
                            create2Txn.getEphemeral() ? header.getClientId() : 0,
                            create2Txn.getParentCVersion(),
                            header.getZxid(),
                            header.getTime(),
                            stat);
                    rc.stat = stat;
                    break;
//                case ZooDefs.OpCode.createTTL:
//                    CreateTTLTxn createTtlTxn = (CreateTTLTxn) txn;
//                    rc.path = createTtlTxn.getPath();
//                    stat = new Stat();
//                    createNode(
//                            createTtlTxn.getPath(),
//                            createTtlTxn.getData(),
//                            createTtlTxn.getAcl(),
//                            EphemeralType.TTL.toEphemeralOwner(createTtlTxn.getTtl()),
//                            createTtlTxn.getParentCVersion(),
//                            header.getZxid(),
//                            header.getTime(),
//                            stat);
//                    rc.stat = stat;
//                    break;
//                case OpCode.createContainer:
//                    CreateContainerTxn createContainerTxn = (CreateContainerTxn) txn;
//                    rc.path = createContainerTxn.getPath();
//                    stat = new Stat();
//                    createNode(
//                            createContainerTxn.getPath(),
//                            createContainerTxn.getData(),
//                            createContainerTxn.getAcl(),
//                            EphemeralType.CONTAINER_EPHEMERAL_OWNER,
//                            createContainerTxn.getParentCVersion(),
//                            header.getZxid(),
//                            header.getTime(),
//                            stat);
//                    rc.stat = stat;
//                    break;
//                case OpCode.delete:
//                case OpCode.deleteContainer:
//                    DeleteTxn deleteTxn = (DeleteTxn) txn;
//                    rc.path = deleteTxn.getPath();
//                    deleteNode(deleteTxn.getPath(), header.getZxid());
//                    break;
//                case OpCode.reconfig:
//                case OpCode.setData:
//                    SetDataTxn setDataTxn = (SetDataTxn) txn;
//                    rc.path = setDataTxn.getPath();
//                    rc.stat = setData(
//                            setDataTxn.getPath(),
//                            setDataTxn.getData(),
//                            setDataTxn.getVersion(),
//                            header.getZxid(),
//                            header.getTime());
//                    break;
//                case OpCode.setACL:
//                    SetACLTxn setACLTxn = (SetACLTxn) txn;
//                    rc.path = setACLTxn.getPath();
//                    rc.stat = setACL(setACLTxn.getPath(), setACLTxn.getAcl(), setACLTxn.getVersion());
//                    break;
//                case OpCode.closeSession:
//                    long sessionId = header.getClientId();
//                    if (txn != null) {
//                        killSession(sessionId, header.getZxid(),
//                                ephemerals.remove(sessionId),
//                                ((CloseSessionTxn) txn).getPaths2Delete());
//                    } else {
//                        killSession(sessionId, header.getZxid());
//                    }
//                    break;
//                case OpCode.error:
//                    ErrorTxn errTxn = (ErrorTxn) txn;
//                    rc.err = errTxn.getErr();
//                    break;
//                case OpCode.check:
//                    CheckVersionTxn checkTxn = (CheckVersionTxn) txn;
//                    rc.path = checkTxn.getPath();
//                    break;
//                case OpCode.multi:
//                    MultiTxn multiTxn = (MultiTxn) txn;
//                    List<Txn> txns = multiTxn.getTxns();
//                    rc.multiResult = new ArrayList<ProcessTxnResult>();
//                    boolean failed = false;
//                    for (Txn subtxn : txns) {
//                        if (subtxn.getType() == OpCode.error) {
//                            failed = true;
//                            break;
//                        }
//                    }
//
//                    boolean post_failed = false;
//                    for (Txn subtxn : txns) {
//                        ByteBuffer bb = ByteBuffer.wrap(subtxn.getData());
//                        Record record = null;
//                        switch (subtxn.getType()) {
//                            case OpCode.create:
//                                record = new CreateTxn();
//                                break;
//                            case OpCode.createTTL:
//                                record = new CreateTTLTxn();
//                                break;
//                            case OpCode.createContainer:
//                                record = new CreateContainerTxn();
//                                break;
//                            case OpCode.delete:
//                            case OpCode.deleteContainer:
//                                record = new DeleteTxn();
//                                break;
//                            case OpCode.setData:
//                                record = new SetDataTxn();
//                                break;
//                            case OpCode.error:
//                                record = new ErrorTxn();
//                                post_failed = true;
//                                break;
//                            case OpCode.check:
//                                record = new CheckVersionTxn();
//                                break;
//                            default:
//                                throw new IOException("Invalid type of op: " + subtxn.getType());
//                        }
//                        assert (record != null);
//
//                        ByteBufferInputStream.byteBuffer2Record(bb, record);
//
//                        if (failed && subtxn.getType() != OpCode.error) {
//                            int ec = post_failed ? Code.RUNTIMEINCONSISTENCY.intValue() : Code.OK.intValue();
//
//                            subtxn.setType(OpCode.error);
//                            record = new ErrorTxn(ec);
//                        }
//
//                        assert !failed || (subtxn.getType() == OpCode.error);
//
//                        TxnHeader subHdr = new TxnHeader(
//                                header.getClientId(),
//                                header.getCxid(),
//                                header.getZxid(),
//                                header.getTime(),
//                                subtxn.getType());
//                        ProcessTxnResult subRc = processTxn(subHdr, record, true);
//                        rc.multiResult.add(subRc);
//                        if (subRc.err != 0 && rc.err == 0) {
//                            rc.err = subRc.err;
//                        }
//                    }
//                    break;
            }
        } catch (Exception e) {
//            LOG.debug("Failed: {}:{}", header, txn, e);
//            rc.err = e.code().intValue();
        }
//        catch (IOException e) {
//            LOG.debug("Failed: {}:{}", header, txn, e);
//        }

        /*
         * Snapshots are taken lazily. When serializing a node, it's data
         * and children copied in a synchronization block on that node,
         * which means newly created node won't be in the snapshot, so
         * we won't have mismatched cversion and pzxid when replaying the
         * createNode txn.
         *
         * But there is a tricky scenario that if the child is deleted due
         * to session close and re-created in a different global session
         * after that the parent is serialized, then when replay the txn
         * because the node is belonging to a different session, replay the
         * closeSession txn won't delete it anymore, and we'll get NODEEXISTS
         * error when replay the createNode txn. In this case, we need to
         * update the cversion and pzxid to the new value.
         *
         * Note, such failures on DT should be seen only during
         * restore.
         */
        if (header.getType() == ZooDefs.OpCode.create) {
//            LOG.debug("Adjusting parent cversion for Txn: {} path: {} err: {}", header.getType(), rc.path, rc.err);
            int lastSlash = rc.path.lastIndexOf('/');
            String parentName = rc.path.substring(0, lastSlash);
            CreateTxn cTxn = (CreateTxn) txn;
            try {
//                setCversionPzxid(parentName, cTxn.getParentCVersion(), header.getZxid());
            } catch (Exception e) {
//                LOG.error("Failed to set parent cversion for: {}", parentName, e);
//                rc.err = e.code().intValue();
            }
        }
//       else if (rc.err != Code.OK.intValue()) {
//            LOG.debug("Ignoring processTxn failure hdr: {} : error: {}", header.getType(), rc.err);
//        }

        /*
         * Things we can only update after the whole txn is applied to data
         * tree.
         *
         * If we update the lastProcessedZxid with the first sub txn in multi
         * and there is a snapshot in progress, it's possible that the zxid
         * associated with the snapshot only include partial of the multi op.
         *
         * When loading snapshot, it will only load the txns after the zxid
         * associated with snapshot file, which could cause data inconsistency
         * due to missing sub txns.
         *
         * To avoid this, we only update the lastProcessedZxid when the whole
         * multi-op txn is applied to DataTree.
         */
        if (!isSubTxn) {
            /*
             * A snapshot might be in progress while we are modifying the data
             * tree. If we set lastProcessedZxid prior to making corresponding
             * change to the tree, then the zxid associated with the snapshot
             * file will be ahead of its contents. Thus, while restoring from
             * the snapshot, the restore method will not apply the transaction
             * for zxid associated with the snapshot file, since the restore
             * method assumes that transaction to be present in the snapshot.
             *
             * To avoid this, we first apply the transaction and then modify
             * lastProcessedZxid.  During restore, we correctly handle the
             * case where the snapshot contains data ahead of the zxid associated
             * with the file.
             */
            if (rc.zxid > lastProcessedZxid) {
                lastProcessedZxid = rc.zxid;
            }

//            if (digestFromLoadedSnapshot != null) {
//                compareSnapshotDigests(rc.zxid);
//            } else {
//                // only start recording digest when we're not in fuzzy state
//                logZxidDigest(rc.zxid, getTreeDigest());
//            }
        }

        return rc;
    }

    /**
     * Add a new node to the DataTree.
     * @param path
     *            Path for the new node.
     * @param data
     *            Data to store in the node.
     * @param acl
     *            Node acls
     * @param ephemeralOwner
     *            the session id that owns this node. -1 indicates this is not
     *            an ephemeral node.
     * @param zxid
     *            Transaction ID
     * @param time
     */
    public void createNode(final String path, byte[] data, List<ACL> acl, long ephemeralOwner, int parentCVersion, long zxid, long time) throws Exception {
        createNode(path, data, acl, ephemeralOwner, parentCVersion, zxid, time, null);
    }

    /**
     * Add a new node to the DataTree.
     * @param path
     *            Path for the new node.
     * @param data
     *            Data to store in the node.
     * @param acl
     *            Node acls
     * @param ephemeralOwner
     *            the session id that owns this node. -1 indicates this is not
     *            an ephemeral node.
     * @param zxid
     *            Transaction ID
     * @param time
     * @param outputStat
     *            A Stat object to store Stat output results into.
     */
    public void createNode(final String path, byte[] data, List<ACL> acl, long ephemeralOwner, int parentCVersion, long zxid, long time, Stat outputStat) throws Exception{
        int lastSlash = path.lastIndexOf('/');
        String parentName = path.substring(0, lastSlash);
        String childName = path.substring(lastSlash + 1);
        StatPersisted stat = createStat(zxid, time, ephemeralOwner);
        DataNode parent = nodes.get(parentName);
        if (parent == null) {
            throw new Exception();
        }
        synchronized (parent) {
            // Add the ACL to ACL cache first, to avoid the ACL not being
            // created race condition during fuzzy snapshot sync.
            //
            // This is the simplest fix, which may add ACL reference count
            // again if it's already counted in in the ACL map of fuzzy
            // snapshot, which might also happen for deleteNode txn, but
            // at least it won't cause the ACL not exist issue.
            //
            // Later we can audit and delete all non-referenced ACLs from
            // ACL map when loading the snapshot/txns from disk, like what
            // we did for the global sessions.
//            Long longval = aclCache.convertAcls(acl);

            Set<String> children = parent.getChildren();
            if (children.contains(childName)) {
                throw new Exception();
            }

            nodes.preChange(parentName, parent);
            if (parentCVersion == -1) {
                parentCVersion = parent.stat.getCversion();
                parentCVersion++;
            }
            // There is possibility that we'll replay txns for a node which
            // was created and then deleted in the fuzzy range, and it's not
            // exist in the snapshot, so replay the creation might revert the
            // cversion and pzxid, need to check and only update when it's
            // larger.
            if (parentCVersion > parent.stat.getCversion()) {
                parent.stat.setCversion(parentCVersion);
                parent.stat.setPzxid(zxid);
            }
            DataNode child = new DataNode(data, 0L, stat);
            parent.addChild(childName);
            nodes.postChange(parentName, parent);
            nodeDataSize.addAndGet(getNodeSize(path, child.data));
            nodes.put(path, child);
//            EphemeralType ephemeralType = EphemeralType.get(ephemeralOwner);
//            if (ephemeralType == EphemeralType.CONTAINER) {
//                containers.add(path);
//            } else if (ephemeralType == EphemeralType.TTL) {
//                ttls.add(path);
//            } else if (ephemeralOwner != 0) {
//                HashSet<String> list = ephemerals.get(ephemeralOwner);
//                if (list == null) {
//                    list = new HashSet<String>();
//                    ephemerals.put(ephemeralOwner, list);
//                }
//                synchronized (list) {
//                    list.add(path);
//                }
//            }
            if (outputStat != null) {
                child.copyStat(outputStat);
            }
        }
        // now check if its one of the zookeeper node child
        if (parentName.startsWith(quotaZookeeper)) {
            // now check if its the limit node
            if (Quotas.limitNode.equals(childName)) {
                // this is the limit node
                // get the parent and add it to the trie
//                pTrie.addPath(Quotas.trimQuotaPath(parentName));
            }
            if (Quotas.statNode.equals(childName)) {
//                updateQuotaForPath(Quotas.trimQuotaPath(parentName));
            }
        }

//        String lastPrefix = getMaxPrefixWithQuota(path);
//        long bytes = data == null ? 0 : data.length;
//        // also check to update the quotas for this node
//        if (lastPrefix != null) {    // ok we have some match and need to update
//            updateQuotaStat(lastPrefix, bytes, 1);
//        }
//        updateWriteStat(path, bytes);
//        dataWatches.triggerWatch(path, Event.EventType.NodeCreated);
//        childWatches.triggerWatch(parentName.equals("") ? "/" : parentName, Event.EventType.NodeChildrenChanged);
    }


    /**
     * Get the size of the nodes based on path and data length.
     *
     * @return size of the data
     */
    public long approximateDataSize() {
        long result = 0;
        for (Map.Entry<String, DataNode> entry : nodes.entrySet()) {
            DataNode value = entry.getValue();
            synchronized (value) {
                result += getNodeSize(entry.getKey(), value.data);
            }
        }
        return result;
    }

    /**
     * Get the size of the node based on path and data length.
     */
    private static long getNodeSize(String path, byte[] data) {
        return (path == null ? 0 : path.length()) + (data == null ? 0 : data.length);
    }

    public static void copyStatPersisted(StatPersisted from, StatPersisted to) {
        to.setAversion(from.getAversion());
        to.setCtime(from.getCtime());
        to.setCversion(from.getCversion());
        to.setCzxid(from.getCzxid());
        to.setMtime(from.getMtime());
        to.setMzxid(from.getMzxid());
        to.setPzxid(from.getPzxid());
        to.setVersion(from.getVersion());
        to.setEphemeralOwner(from.getEphemeralOwner());
    }


    /**
     * Create a node stat from the given params.
     *
     * @param zxid the zxid associated with the txn
     * @param time the time when the txn is created
     * @param ephemeralOwner the owner if the node is an ephemeral
     * @return the stat
     */
    public static StatPersisted createStat(long zxid, long time, long ephemeralOwner) {
        StatPersisted stat = new StatPersisted();
        stat.setCtime(time);
        stat.setMtime(time);
        stat.setCzxid(zxid);
        stat.setMzxid(zxid);
        stat.setPzxid(zxid);
        stat.setVersion(0);
        stat.setAversion(0);
        stat.setEphemeralOwner(ephemeralOwner);
        return stat;
    }
}
