package org.example.appenders.kafka;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.lang3.StringUtils;
import org.example.producer.ProducerTest;
import org.example.utils.ThreadUtils;

import java.lang.ref.SoftReference;
import java.util.concurrent.*;

import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.example.utils.Utils.debugLog;

public class KafkaWorker implements Worker<ByteMessage>,
        EventHandler<KafkaEvent>,
        SequenceReportingEventHandler<KafkaEvent>,
        BatchStartAware,
        TimeoutHandler {
    private final ScheduledFuture<?> connectFuture;
    private final String url;
    private final String topic;
    private RingBuffer<KafkaEvent> ringBuffer;
    private final Worker<Object> logWorker;
    private final Disruptor<KafkaEvent> queue;
    private static final int BUFFER_SIZE = 256;
    private static final ThreadFactory THREAD_FACTORY = ThreadUtils.namedDaemonThreadFactory("log-pulsar-worker");
    private static final int BATH_MESSAGE_SIZE = 128;
    private static ProducerTest producer;
    private Sequence sequenceCallback;

    public KafkaWorker(Worker<Object> logWorker, String url, String topic) {
        this.logWorker = logWorker;
        queue = new Disruptor<>(
                KafkaEvent::new,
                BUFFER_SIZE,
                THREAD_FACTORY,
                ProducerType.SINGLE,
                new LiteTimeoutBlockingWaitStrategy(1, SECONDS)

        );
        queue.handleEventsWith(this);
        ringBuffer = queue.getRingBuffer();

        if (StringUtils.isNotBlank(topic)) {
            topic = topic.toLowerCase();
        }
        this.url = url;
        this.topic = topic;
        connectFuture = scheduleWithFixedDelay(this::connect, 0, 5, SECONDS);
    }

    private void connect() {
        try {
            producer = new ProducerTest();
        } catch (Exception ignored) {
        }
        queue.start();
        connectFuture.cancel(true);
    }

    @Override
    public void onBatchStart(long batchSize) {

    }

    @Override
    public void setSequenceCallback(Sequence sequenceCallback) {

    }

    private long lastMessageId;

    private long messageCount;
    private int batchIndex;
    @Override
    public void onEvent(KafkaEvent event, long sequence, boolean endOfBatch) {

        ByteEvent byteEvent = event.getByteEvent();
        int eventSize = byteEvent.getBufferLen();
        long lastMessageId = byteEvent.getId();
        if (eventSize > 5_000_000) {
            debugLog("日志过大, 直接丢弃:" + event.getEventString());
        } else {
            producer.push("log", event.getEventString(), null, callback ->{});
        }
        if (++batchIndex >= BATH_MESSAGE_SIZE || endOfBatch) {
            try {
                debugLog("producer.flush()");
//                producer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
//            sequenceCallback.set(sequence);
            batchIndex = 0;
        }
        if (endOfBatch) {
            this.lastMessageId = lastMessageId;
            onTimeout(lastMessageId);
        }
        messageCount++;
    }

    private long nextSendSeqTime;
    private long lastSendSeqId;

    @Override
    public void onTimeout(long sequence) {
        if (sequence < 0) {
            return;
        }
        if (lastSendSeqId != lastMessageId || (nextSendSeqTime < currentTimeMillis())) {
//            debugLog("sync seq:" + lastMessageId);
            logWorker.sendMessage(new LastSeq(lastMessageId));
            nextSendSeqTime = currentTimeMillis() + 1000;
            lastSendSeqId = sequence;
        }
    }

    private volatile boolean isDisposed = false;
    @Override
    public boolean sendMessage(ByteMessage message) {
        return !isDisposed && ringBuffer.tryPublishEvent((e, s) -> {
            ByteEvent byteEvent = e.getByteEvent();
            message.apply(byteEvent);
        });
    }

    @Override
    public void close() {

    }

    private final static ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(5);
    public static ScheduledFuture<?> scheduleWithFixedDelay(RethrowRunnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduledPool.scheduleWithFixedDelay(RethrowRunnable.runWithCatch(command), initialDelay, delay, unit);
    }
}

class KafkaEvent {
    private SoftReference<ByteEvent> byteEvent = new SoftReference<>(new ByteEvent());

    private ByteEvent refKeep;
    ByteEvent getByteEvent() {
        if (nonNull(refKeep)) {
            return refKeep;
        }
        ByteEvent event = this.byteEvent.get();
        if (isNull(event)) {
            event = new ByteEvent();
            byteEvent = new SoftReference<>(event);
        }
        refKeep = event;
        return event;
    }

    String getEventString() {
        if (isNull(refKeep)) {
            return "";
        }
        return new String(refKeep.getBuffer().array(), 0, refKeep.getBufferLen(), UTF_8);
    }
}

interface RethrowRunnable {
    void run() throws Throwable;

    static Runnable runWithCatch(RethrowRunnable fun) {
        return () -> {
            try {
                fun.run();
            } catch (Throwable e) {
                debugLog("execute error, but sneaky catch" + e);
            }
        };
    }
}