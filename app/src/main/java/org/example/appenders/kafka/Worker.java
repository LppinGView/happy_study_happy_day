package org.example.appenders.kafka;

import lombok.Data;
import lombok.Value;

import java.io.Closeable;
import java.nio.ByteBuffer;

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

//todo 这是做啥的
abstract class ByteMessage {
    private long id;

    public abstract void apply(ByteEvent event);

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}

@Value
class LastSeq {
    private long seq;
}