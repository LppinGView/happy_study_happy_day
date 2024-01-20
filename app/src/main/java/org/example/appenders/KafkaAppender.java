package org.example.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.example.appenders.kafka.KafkaAppenderFactory;
import org.example.appenders.kafka.MDCConvertToByteMessage;
import org.example.appenders.kafka.Worker;
import org.example.producer.ProducerTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class KafkaAppender<E> extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final Logger log = LoggerFactory.getLogger(KafkaAppender.class);
    private static ProducerTest msCenter;
    private static ExecutorService executorService;
    private String url;
    private String topic;
    private boolean enable;
    private Map<String, String> additionalFields = new HashMap<>();
    private MDCConvertToByteMessage mdcConvertToByteMessage;
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private static volatile Worker<Object> wrapper;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private static volatile int instanceCount;

//    public KafkaAppender(String url, String topic, boolean enable){
//        this.enable = enable;
//        this.url = url;
//        this.topic = topic;
//    }
//    private

    @Override
    protected void append(ILoggingEvent eventObject) {
        log.debug("append kafka...");

//        if (enable && nonNull(wrapper)) {
            wrapper.sendMessage(eventObject);
//        }

//        executorService.submit(()->{
//            msCenter.push("app_server", iLoggingEvent.getMessage(), null, Handler::doAction);
//        });
    }

    @Override
    public void start() {
        super.start();
        synchronized (KafkaAppender.class){
            System.out.println("kafka appender url:" + url);
            System.out.println("kafka appender topic:" + topic);
            System.out.println("kafka appender enable:" + enable);

            instanceCount++;
//            if (enable && isNotBlank(url) && isNotBlank(topic) && isNull(wrapper)) {
                System.out.println("kafka appender created!");
                try {
                    wrapper = KafkaAppenderFactory.create(null, url, topic, additionalFields, mdcConvertToByteMessage);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
//            }

//            if (enable && isNotBlank(url) && isNotBlank(topic)) {
//                System.out.println("kafka appender created!");
//                executorService = Executors.newFixedThreadPool(1);
//                msCenter = new ProducerTest();
//            }
        }
    }
}

class Handler{
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    public static void doAction(String record){
//        log.info("get callback info: {}", record);
    }
}
