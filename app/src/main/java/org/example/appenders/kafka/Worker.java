package org.example.appenders.kafka;

import lombok.Data;
import lombok.Value;

import java.io.Closeable;
import java.nio.ByteBuffer;

import static java.lang.System.arraycopy;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public interface Worker<T> extends Closeable {
    boolean sendMessage(T message);
    void close();
}

//what it is
@Data
class ByteEvent {
    private long id;
    private ByteBuffer buffer;
    private int bufferLen;
    public ByteBuffer getBuffer(){
        return nonNull(buffer)? buffer : ByteBuffer.allocateDirect(5);
    }
    public void clear() {
        if (nonNull(buffer)) {
            buffer.clear();
        }
        bufferLen = 0;
        id = 0;
    }
}

@Data
//todo 这是做啥的
abstract class ByteMessage {
    private long id;

    public abstract void apply(ByteEvent event);
}

@Data
class DataByteMessage extends ByteMessage {
    private byte[] data;
    private int dataLength;

    public DataByteMessage(long id, byte[] data) {
        this.setId(id);
        this.data = data;
        this.dataLength = nonNull(data) ? data.length : 0;
    }

    public DataByteMessage(long id, byte[] data, int len) {
        this.setId(id);
        this.data = data;
        this.dataLength = len;
    }

    public void apply(ByteEvent event) {
        event.clear();
        if (isNull(data) || 0 == dataLength) {
            return;
        }
        event.setId(this.getId());
        ByteBuffer buffer = event.getBuffer();
        if (isNull(buffer) || buffer.capacity() < dataLength) {
            buffer = ByteBuffer.allocate(marginToBuffer(dataLength));
            event.setBuffer(buffer);
        }
        arraycopy(data, 0, buffer.array(), 0, dataLength);
        event.setBufferLen(dataLength);
    }

    /**
     * 缓存行64对齐
     */
    public static int marginToBuffer(int len) {
        if ((len & 63) != 0) {
            len &= ~63;
            len += 64;
        }
        return len;
    }

}

@Value
class LastSeq {
    private long seq;
}