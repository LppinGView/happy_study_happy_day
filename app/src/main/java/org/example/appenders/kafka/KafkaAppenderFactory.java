package org.example.appenders.kafka;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

import java.util.Map;

import static java.util.Objects.nonNull;

public class KafkaAppenderFactory {
    public static Worker<Object> create(
            PatternLayoutEncoder encoder,
            String url,
            String topic,
            Map<String, String> additionalFields,
            MDCConvertToByteMessage mdcConvertToByteMessage
    ){
        return new AppenderFacade(encoder, url, topic, additionalFields, mdcConvertToByteMessage);
    }
}

//这里封一层代理， 即使实例还未创建， 上层初始化不会有啥问题。隔离了下层的具体逻辑
class WorkerRef<T> implements Worker<T>{
    private Worker<T> delegate;

    public void setDelegate(Worker<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean sendMessage(T message) {
        if (nonNull(delegate)){
            return delegate.sendMessage(message);
        }
        return false;
    }

    @Override
    public void close() {
        if (nonNull(delegate)){
            delegate.close();
            delegate = null;
        }
    }
}

class AppenderFacade implements Worker<Object>{
    private WorkerRef<Object> logWorker = new WorkerRef<>();
    private WorkerRef<ByteMessage> kafkaWorker = new WorkerRef<>();
    private WorkerRef<Object> fileQueueWorker = new WorkerRef<>();

    public AppenderFacade(
            PatternLayoutEncoder encoder,
            String url,
            String topic,
            Map<String, String> additionalFields,
            MDCConvertToByteMessage mdcConvertToByteMessage
    ){
        try {
            fileQueueWorker.setDelegate(
                    new FileQueueWorker(kafkaWorker)
            );

            logWorker.setDelegate(
                    new LogWorker(encoder, kafkaWorker, fileQueueWorker, additionalFields, mdcConvertToByteMessage)
            );

//            if (isNoneBlank(url) && isNotBlank(topic)) {
                kafkaWorker.setDelegate(
                        new KafkaWorker(logWorker, url, topic)
                );
//            }
        } catch (Exception ex) {
            this.close();
            throw ex;
        }
    }

    @Override
    public boolean sendMessage(Object message) {
        return logWorker.sendMessage(message);
    }

    @Override
    public void close() {
        logWorker.close();
        kafkaWorker.close();
        fileQueueWorker.close();
    }


}