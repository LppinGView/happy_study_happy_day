package com.lpp.demo.copyZoo.zookeeper.proto;

import com.lpp.demo.copyZoo.jute.*;

public class ConnectRequest implements Record {
    private int protocolVersion;
    private long lastZxidSeen;
    private int timeOut;
    private long sessionId;
    private byte[] passwd;
    private boolean readOnly;
    public ConnectRequest() {
    }
    public ConnectRequest(
            int protocolVersion,
            long lastZxidSeen,
            int timeOut,
            long sessionId,
            byte[] passwd,
            boolean readOnly) {
        this.protocolVersion=protocolVersion;
        this.lastZxidSeen=lastZxidSeen;
        this.timeOut=timeOut;
        this.sessionId=sessionId;
        this.passwd=passwd;
        this.readOnly=readOnly;
    }
    public int getProtocolVersion() {
        return protocolVersion;
    }
    public void setProtocolVersion(int m_) {
        protocolVersion=m_;
    }
    public long getLastZxidSeen() {
        return lastZxidSeen;
    }
    public void setLastZxidSeen(long m_) {
        lastZxidSeen=m_;
    }
    public int getTimeOut() {
        return timeOut;
    }
    public void setTimeOut(int m_) {
        timeOut=m_;
    }
    public long getSessionId() {
        return sessionId;
    }
    public void setSessionId(long m_) {
        sessionId=m_;
    }
    public byte[] getPasswd() {
        return passwd;
    }
    public void setPasswd(byte[] m_) {
        passwd=m_;
    }
    public boolean getReadOnly() {
        return readOnly;
    }
    public void setReadOnly(boolean m_) {
        readOnly=m_;
    }
    public void serialize(OutputArchive a_, String tag) throws java.io.IOException {
        a_.startRecord(this,tag);
        a_.writeInt(protocolVersion,"protocolVersion");
        a_.writeLong(lastZxidSeen,"lastZxidSeen");
        a_.writeInt(timeOut,"timeOut");
        a_.writeLong(sessionId,"sessionId");
        a_.writeBuffer(passwd,"passwd");
        a_.writeBool(readOnly,"readOnly");
        a_.endRecord(this,tag);
    }
    public void deserialize(InputArchive a_, String tag) throws java.io.IOException {
        a_.startRecord(tag);
        protocolVersion=a_.readInt("protocolVersion");
        lastZxidSeen=a_.readLong("lastZxidSeen");
        timeOut=a_.readInt("timeOut");
        sessionId=a_.readLong("sessionId");
        passwd=a_.readBuffer("passwd");
        readOnly=a_.readBool("readOnly");
        a_.endRecord(tag);
    }
    public String toString() {
        try {
            java.io.ByteArrayOutputStream s =
                    new java.io.ByteArrayOutputStream();
            ToStringOutputArchive a_ =
                    new ToStringOutputArchive(s);
            a_.startRecord(this,"");
            a_.writeInt(protocolVersion,"protocolVersion");
            a_.writeLong(lastZxidSeen,"lastZxidSeen");
            a_.writeInt(timeOut,"timeOut");
            a_.writeLong(sessionId,"sessionId");
            a_.writeBuffer(passwd,"passwd");
            a_.writeBool(readOnly,"readOnly");
            a_.endRecord(this,"");
            return new String(s.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }
    public void write(java.io.DataOutput out) throws java.io.IOException {
        BinaryOutputArchive archive = new BinaryOutputArchive(out);
        serialize(archive, "");
    }
    public void readFields(java.io.DataInput in) throws java.io.IOException {
        BinaryInputArchive archive = new BinaryInputArchive(in);
        deserialize(archive, "");
    }
    public int compareTo (Object peer_) throws ClassCastException {
        if (!(peer_ instanceof ConnectRequest)) {
            throw new ClassCastException("Comparing different types of records.");
        }
        ConnectRequest peer = (ConnectRequest) peer_;
        int ret = 0;
        ret = (protocolVersion == peer.protocolVersion)? 0 :((protocolVersion<peer.protocolVersion)?-1:1);
        if (ret != 0) return ret;
        ret = (lastZxidSeen == peer.lastZxidSeen)? 0 :((lastZxidSeen<peer.lastZxidSeen)?-1:1);
        if (ret != 0) return ret;
        ret = (timeOut == peer.timeOut)? 0 :((timeOut<peer.timeOut)?-1:1);
        if (ret != 0) return ret;
        ret = (sessionId == peer.sessionId)? 0 :((sessionId<peer.sessionId)?-1:1);
        if (ret != 0) return ret;
        {
            byte[] my = passwd;
            byte[] ur = peer.passwd;
            ret = Utils.compareBytes(my,0,my.length,ur,0,ur.length);
        }
        if (ret != 0) return ret;
        ret = (readOnly == peer.readOnly)? 0 : (readOnly?1:-1);
        if (ret != 0) return ret;
        return ret;
    }
    public boolean equals(Object peer_) {
        if (!(peer_ instanceof ConnectRequest)) {
            return false;
        }
        if (peer_ == this) {
            return true;
        }
        ConnectRequest peer = (ConnectRequest) peer_;
        boolean ret = false;
        ret = (protocolVersion==peer.protocolVersion);
        if (!ret) return ret;
        ret = (lastZxidSeen==peer.lastZxidSeen);
        if (!ret) return ret;
        ret = (timeOut==peer.timeOut);
        if (!ret) return ret;
        ret = (sessionId==peer.sessionId);
        if (!ret) return ret;
        ret = java.util.Arrays.equals(passwd,peer.passwd);
        if (!ret) return ret;
        ret = (readOnly==peer.readOnly);
        if (!ret) return ret;
        return ret;
    }
    public int hashCode() {
        int result = 17;
        int ret;
        ret = (int)protocolVersion;
        result = 37*result + ret;
        ret = java.lang.Long.hashCode(lastZxidSeen);
        result = 37*result + ret;
        ret = (int)timeOut;
        result = 37*result + ret;
        ret = java.lang.Long.hashCode(sessionId);
        result = 37*result + ret;
        ret = java.util.Arrays.hashCode(passwd);
        result = 37*result + ret;
        ret = java.lang.Boolean.hashCode(readOnly);
        result = 37*result + ret;
        return result;
    }
    public static String signature() {
        return "LConnectRequest(ililBz)";
    }
}
