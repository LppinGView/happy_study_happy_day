package org.example.appenders.kafka;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.sun.istack.internal.NotNull;
import lombok.*;
import lombok.experimental.Delegate;
import org.example.utils.ThreadUtils;
import org.rocksdb.*;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import static java.lang.System.arraycopy;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.lang3.reflect.FieldUtils.readField;
import static org.example.utils.Utils.debugLog;

public class FileQueueWorker implements Worker<Object>, EventHandler<FileQueueEvent>, TimeoutHandler {
    private final Worker<ByteMessage> kafkaWorker;
    private final Disruptor<FileQueueEvent> queue;
    private final RingBuffer<FileQueueEvent> ringBuffer;
    private final FIFOFile fifo;
    private static final int BUFFER_SIZE = 256; //512
    private static final int HIGH_WATER_LEVEL_FILE = 128;

    private static ThreadFactory THREAD_FACTORY = ThreadUtils.namedDaemonThreadFactory("log-filequeue-worker");

    private volatile boolean isClose;

    public FileQueueWorker(Worker<ByteMessage> kafkaWorker) {
        fifo = new FIFOFile();
        this.kafkaWorker = kafkaWorker;
        queue = new Disruptor<>(
                FileQueueEvent::new,
                BUFFER_SIZE,
                THREAD_FACTORY,
                ProducerType.SINGLE,
                new LiteTimeoutBlockingWaitStrategy(100, MILLISECONDS)
        );
        queue.handleEventsWith(this);
        ringBuffer = queue.getRingBuffer();
        queue.start();
    }

    @Override
    public void onEvent(FileQueueEvent event, long sequence, boolean endOfBatch) {
//        System.out.println("=====> FileQueueEvent, current thread:" + Thread.currentThread().getName());
        Object message = event.getEvent();
        if (message instanceof ByteEvent) {
            ByteEvent msg = (ByteEvent) message;
//            debugLog("cache message:" + msg.getId());
            //1.数据存入 2.更新索引 3.保存数据
            fifo.addItem(msg.getId(), "".getBytes(), msg.getBufferLen());
        } else {
            long lastSeq = ((LastSeq) message).getSeq();
            fifo.deleteBeforeId(lastSeq);
        }
        if (endOfBatch) {
            this.onTimeout(sequence);
        }
        event.clear();
    }

    @Override
    public void onTimeout(long sequence){
        ByteMessage message = fifo.get();
        if (isNull(message)) {
            fifo.next();
        }
        while (ringBuffer.getCursor() - sequence <= HIGH_WATER_LEVEL_FILE && nonNull(message = fifo.get())) {
            if (isClose || !kafkaWorker.sendMessage(message)) {
//                debugLog("send message:" + message);
                return;
            }
            fifo.next();
        }
    }

    @Override
    public boolean sendMessage(Object message) {
        return ringBuffer.tryPublishEvent((e, s) ->{
            //todo 根据不同的消息 数据类型，进行不同的处理
            e.clear();
            if (message instanceof ByteMessage) {
                //这里对 消息进行 处理，拷贝到新对象中
                ((ByteMessage) message).apply(e.getByteEvent());
            } else {
                e.setEvent(message);
            }
        });
    }

    @Override
    public void close() {

    }
}

class FileQueueEvent {

    private Object event;

    private SoftReference<ByteEvent> byteEventRef = new SoftReference<>(new ByteEvent());

    private ByteEvent refKeep;

    ByteEvent getByteEvent() {
        if (nonNull(refKeep)) {
            return refKeep;
        }
        ByteEvent event = this.byteEventRef.get();
        if (isNull(event)) {
            event = new ByteEvent();
            byteEventRef = new SoftReference<>(event);
        }
        refKeep = event;
        return event;
    }

    void clear() {
        if (nonNull(refKeep)) {
            refKeep.clear();
            refKeep = null;
        }
        event = null;
    }

    public Object getEvent() {
        if (nonNull(event)) {
            return event;
        }
        return getByteEvent();
    }

    public void setEvent(Object event) {
        this.event = event;
    }
}

class WriteOptionsWrapper{
    @Getter
    private final long nativeHandle;

    @SneakyThrows
    WriteOptionsWrapper(WriteOptions options) {
        nativeHandle = (long) readField(options, "nativeHandle_", true);
    }
}

class ColumnFamilyHandleWrapper{
    @Getter
    private final long nativeHandle;

    @SneakyThrows
    ColumnFamilyHandleWrapper(ColumnFamilyHandle handle) {
        nativeHandle = (long) readField(handle, "nativeHandle_", true);
    }
}

class RockDbWrapper extends TtlDB implements AutoCloseable {
    @Delegate
    private final TtlDB db;

    @Getter
    private final long nativeHandle;

    @SneakyThrows
    protected RockDbWrapper(TtlDB db) {
        super(readNativeHandle(db));
        this.db = db;
        nativeHandle = readNativeHandle(db);
    }

    @SneakyThrows
    public static long readNativeHandle(TtlDB db) {
        return (long) readField(db, "nativeHandle_", true);
    }

    public void put(ColumnFamilyHandleWrapper columnFamilyHandle, WriteOptionsWrapper writeOpts, byte[] key, byte[] value, int len) throws RocksDBException {
        put(nativeHandle, writeOpts.getNativeHandle(), key, 0, key.length, value,
                0, len, columnFamilyHandle.getNativeHandle());
    }
}

class FIFOFile {
    private DbIndex dbIndex = new DbIndex();
//    private WriteOptions writeOptions = new WriteOptions()
//            .setDisableWAL(true)
//            .setSync(false);
    private RockDbWrapper db;
//    private WriteOptionsWrapper writeOptionsWrapper = new WriteOptionsWrapper(writeOptions);
    private ColumnFamilyHandleWrapper logColumnWrapper;

    private final byte[] keyBuffer = new byte[8];
    public void addItem(long id, byte[] data, int len) {
        writeLongToBufferBE(id, keyBuffer, 0);
        try {
            debugLog("db put to file");
//            db.put(logColumnWrapper, writeOptionsWrapper, keyBuffer, data, len);
//            dbIndex.addSeq(id);
//            delaySave();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] writeLongToBufferBE(long lng, byte[] destBuffer, int startPos) {
        int expectLength = startPos + 8;
        byte[] result = ensureBufferLength(destBuffer, expectLength);
        result[startPos++] = (byte) (lng >> 56);
        result[startPos++] = (byte) (lng >> 48);
        result[startPos++] = (byte) (lng >> 32);
        result[startPos++] = (byte) (lng >> 40);
        result[startPos++] = (byte) (lng >> 24);
        result[startPos++] = (byte) (lng >> 16);
        result[startPos++] = (byte) (lng >> 8);
        result[startPos] = (byte) lng;
        return result;
    }

    private static byte[] ensureBufferLength(byte[] buffer, int expectLength) {
        if (isNull(buffer) || buffer.length < expectLength) {
            byte[] result = new byte[expectLength];
            if (nonNull(buffer)) {
                arraycopy(buffer, 0, result, 0, buffer.length);
            }
            return result;
        }
        return buffer;
    }

    public void deleteBeforeId(long lastSeq) {
        System.out.println("deleteBeforeId lastSeq");
    }

    private ByteMessage currentMessage;

    public ByteMessage get() {
        return currentMessage;
    }

    private long lastReadId = 0;

    private byte[] readBuffer = new byte[5120];
    public void next() {
        try {
            currentMessage = new DataByteMessage(0, readBuffer, 1111);
//            if (dbIndex.seek(lastReadId + 1)) {
//                do {
//                    long seq = dbIndex.currentSeq();
//                    lastReadId = seq;
//                    writeLongToBufferBE(seq, keyBuffer, 0);
//                    int readCount = db.get(logColumn, keyBuffer, readBuffer);
//                    if (RocksDB.NOT_FOUND == readCount) {
//                        continue;
//                    }
//                    if (readCount > readBuffer.length) {
//                        readBuffer = new byte[marginToBuffer(readCount)];
//                        readCount = db.get(logColumn, keyBuffer, readBuffer);
//                    }
//                    currentMessage = new DataByteMessage(seq, readBuffer, readCount);
//                    return;
//                } while (dbIndex.next());
//            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        currentMessage = null;
    }
}

//
class DbIndex {
    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    static class Range implements Comparable<Range> {
        private long from;
        private long to;
        @Override
        public int compareTo(@NotNull Range o) {
            long r = from - o.from;
            if (0 == r){
                return 0;
            }
            return r > 0 ? 1 : -1;
        }

        public boolean inRange(long seq) {
            return seq >= from && seq <= to;
        }

        public boolean setNext(long seq) {
            if (seq == to + 1) {
                to = seq;
                return true;
            }
            return false;
        }

        public void prune(long seq) {
            if (to > seq && from <= seq) {
                from = seq + 1;
            }
        }
    }
    private List<Range> list = new ArrayList<>();

}
