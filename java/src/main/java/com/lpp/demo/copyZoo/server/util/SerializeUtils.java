package com.lpp.demo.copyZoo.server.util;

import com.lpp.demo.copyZoo.jute.InputArchive;
import com.lpp.demo.copyZoo.jute.OutputArchive;
import com.lpp.demo.copyZoo.server.DataTree;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SerializeUtils {
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
