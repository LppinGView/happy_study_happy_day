package org.example.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import org.example.producer.ProducerTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaAppender<E> extends UnsynchronizedAppenderBase<ILoggingEvent> {
    private static final Logger log = LoggerFactory.getLogger(KafkaAppender.class);
    private static ProducerTest msCenter;
    private static ExecutorService executorService;

    @Override
    protected void append(ILoggingEvent iLoggingEvent) {
        log.info("append kafka...");
        executorService.submit(()->{
            msCenter.push("app_server", iLoggingEvent.getMessage(), null, Handler::doAction);
        });
    }

    @Override
    public void start() {
        super.start();
        executorService = Executors.newFixedThreadPool(1);
        msCenter = new ProducerTest();
    }
}

class Handler{
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    public static void doAction(String record){
        log.info("get callback info: {}", record);
    }
}
