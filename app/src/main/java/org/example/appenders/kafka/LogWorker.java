package org.example.appenders.kafka;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.Data;
import lombok.val;
import org.example.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.LongAdder;

import static java.lang.System.currentTimeMillis;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.startsWithAny;

public class LogWorker implements Worker<Object>,
        EventHandler<LogEvent>,
        SequenceReportingEventHandler<LogEvent>,
        BatchStartAware {

    private Logger log = LoggerFactory.getLogger(LogWorker.class);

    private final Disruptor<LogEvent> queue;
    private final RingBuffer<LogEvent> producer;
    // 缓冲区设置
    // 初始的缓冲池, 避免短期内日志突然增多造成日志来不及处理而丢失
    // 本实例是日志的入口, 尽量通过缓冲区把各个线程的日志的平缓的收集过来
    private static final int BUFFER_SIZE = 1 << 12; // 4k   队列中数据个数
    // 日志有两个可能方向, 一个往pulsar, 一个写本地文件缓存
    // 以下两个是高水位阈值, 日志堆积超过这个阈值后应该丢弃之前的日志,
    // 这种策略同时能达到两个目地:
    //   1. 缓冲区空间足够的时候尽可能的等后续渠道消费日志, 空间紧张时预先丢弃不重要的日志
    //   2. 日志和同步SEQ都是通过消息发送, 但消息优先级应该更高, 通过丢弃之前的同步SEQ消息使得消息能更快被处理
    static final int HIGH_WATER_LEVEL_PULSAR = 2 * 1024; // 2k
    static final int HIGH_WATER_LEVEL_FILE = BUFFER_SIZE - 1024; // 3K
    private static ThreadFactory THREAD_FACTORY = ThreadUtils.namedDaemonThreadFactory("log-log-worker");
    private volatile boolean isClosed = false;
    private final Worker<Object> fileQueueWorker;
    private final Worker<ByteMessage> pulsarWorker;
    // 消息去向, 二选1
    // 初始时先通过file,file缓冲区为空的切到pulsar
    // pulsar堵塞的时候切到file缓存
    private boolean directWriteToPulsar = false;
    private final LongAdder missingCount = new LongAdder();

    public LogWorker(PatternLayoutEncoder encoder, Worker<ByteMessage> pulsarWorker, Worker<Object> fileQueueWorker, Map<String, String> additionalFields, MDCConvertToByteMessage mdcConvertToByteMessage){
        this.pulsarWorker = pulsarWorker;
        this.fileQueueWorker = fileQueueWorker;
        queue = new Disruptor<>(
                LogEvent::new,
                BUFFER_SIZE,
                THREAD_FACTORY,
                ProducerType.MULTI, // 注意此处为多生产者
//            new PhasedBackoffWaitStrategy(100, 10_000, MICROSECONDS, new LiteBlockingWaitStrategy())
                new LiteBlockingWaitStrategy() //cpu自旋，避免线程切换
        );
        queue.handleEventsWith(this);
        producer = queue.getRingBuffer();
        queue.start();
    }

    // 日志id, 发送成功一条加1,
    // 初始值 [ currentMillSeconds, 0 ]
    //       [  41bit,    12bit       ]
    private long lastMessageId = currentTimeMillis() << 12; //当前内存队列大小  2^12 个

    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch){
        Object eventLog = event.getLog();
        boolean success = false;

        // 普通的日志
        long messageId = lastMessageId + 1;
        event.setId(messageId);

        if (directWriteToPulsar){
            while (!(success = pulsarWorker.sendMessage(event))){
                if (isClosed) {
                    break;
                }
                if (producer.getCursor() - sequence >= HIGH_WATER_LEVEL_PULSAR) {
                    //todo 这里触达高水位，直接丢弃消息
                    break;
                }
                ThreadUtils.sleep(5);
            }
            // 写入失败, 切换到本地文件缓冲区
            if (!success && directWriteToPulsar) {
                directWriteToPulsar = false;
                log.debug("pulsar阻塞,切换到file cache");
            }
        }

        //todo 刚开始，不清楚pulsar是否连通，这里先走file
        if (!directWriteToPulsar){
            while (!(success = fileQueueWorker.sendMessage(event))){
                if (isClosed) {
                    break;
                }
                if (producer.getCursor() - sequence >= HIGH_WATER_LEVEL_FILE) {
                    break;
                }
                ThreadUtils.sleep(5);
            }
            if (!success){
                missingCount.increment();
            }
        }
    }

    @Override
    public void onBatchStart(long l) {

    }

    @Override
    public void setSequenceCallback(Sequence sequence) {

    }

    private boolean isExclude(ILoggingEvent message) {
        if (isNull(message)) {
            return true;
        }
        return startsWithAny(
                message.getLoggerName(),
                "ch.qos.logback",
                "org.apache.pulsar",
                "org.rocksdb"
        );
    }

//    public JsonByteBuilder getByteBuilder() {
//        if (null == byteBuilder) {
//            byteBuilder = cache.get();
//            if (null == byteBuilder) {
//                byteBuilder = JsonByteBuilder.create(2048);
//            }
//        }
//        return byteBuilder;
//    }

    @Override
    public boolean sendMessage(Object message) {
        if (message instanceof ILoggingEvent) {
            val msg = (ILoggingEvent) message;
            if (isExclude(msg)) {
                return true;
            }
            if (!producer.tryPublishEvent((event, sequence) -> {
                //todo msg to JSON, then publishEvent
//                msg.getMessage();
//                convertToByteMessage(msg, event.getByteBuilder());
            })) {
                missingCount.increment();
                return false;
            } else {
                return true;
            }
        } else {
            return producer.tryPublishEvent((event, sequence) -> {
                event.setLog(message);
            });
        }
    }

    @Override
    public void close() {

    }
}

@Data
class LogEvent extends ByteMessage{
    private Object log;
//    private JsonByteBuilder byteBuilder = JsonByteBuilder.create(2 * 1024);

    @Override
    public void apply(ByteEvent event) {
//        event.clear();
//        event.setId(this.getId());
//        ByteBuffer buff = byteBuilder.toByteBuffer(event.getBuffer());
//        event.setBuffer(buff);
//        event.setBufferLen(buff.position());
    }
}
