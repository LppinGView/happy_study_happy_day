package com.redis.demo.core.event;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.NONE;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

public interface DomainEventReceiver {
    void registerCallback(String serviceName, String subscribeName, EventReceiver callback);

    void registerCallback(String subscribeName, EventReceiver callback);

    default EventSubscriber subscriber(String serviceName, String subscribeName) {
        return new EventSubscriber(serviceName, serviceName, this);
    }

    class DomainEventMessage {
        private final static Pattern SOURCE = Pattern.compile("\\.[^.]+$");
        @NonNull
        private String topicName;
        @NonNull
        private String type;

        private String source;

        private String key;

        private String domainName;

        @NonFinal
        @Getter(NONE)
        private Message<byte[]> msg;

        //？volatile
        @NonFinal
        @Getter(NONE)
        private volatile String msgStr;

        //？volatile
        //?rawtypes
        @SuppressWarnings("rawtypes")
        @NonFinal
        @Getter(NONE)
        private volatile DomainEvent event;

        //?
        @NonFinal
        @Getter(NONE)
        private Class<?> typeClass = UnInitClass.class;

        @NonFinal
        private int retryIndex = 0;

        private static class UnInitClass {}

        //? WeakerAccess
        @SuppressWarnings("WeakerAccess")
        public <T> boolean isType(Class<T> cls) {
            if (isNull(cls)) {
                return false;
            }
            if (cls.getName().equals(type)){
                return true;
            }
            //这个是做什么用？类还未初始化?
            if (UnInitClass.class.equals(typeClass)){
                //尝试初始化?
                try {
                    typeClass = Class.forName(type);
                } catch (ClassNotFoundException e) {
                    typeClass = null;
                }
            }
            if (nonNull(typeClass)){
                return cls.isAssignableFrom(typeClass);
            }
            return false;
        }

        public boolean isType(Pattern pattern){
            if (isNull(pattern){
                return false;
            }
            return pattern.matcher(type).matches();
        }

        public boolean isSource(Pattern pattern){
            if (isNull(pattern) || isBlank(source)){
                return false;
            }
            return pattern.matcher(source).matches();
        }

        public boolean isSource(String name) {
            if (isBlank(name)) {
                return false;
            }
            return name.equals(source);
        }

        public boolean isSourceName(String name) {
            if (isBlank(name)) {
                return false;
            }
            return name.equals(getSourceName());
        }

        public String getSourceName() {
            return substringAfterLast(source, ".");
        }

        @Builder
        public DomainEventMessage(
            String topicName,
            String type,
            String key,
            String domainName,
            Message<byte[]> msg,
            String source,
            int retryIndex
        ){
            this.topicName = topicName;
            this.type = type;
            this.msg = msg;
            this.source = source;
            this.key = key;
            this.domainName = domainName;
            this.retryIndex = retryIndex;
        }

        private String getMsgStr() {
            if (isNull(msgStr) && nonNull(msg)){
                byte[] msgBytes = msg.getValue();
                new String(msgBytes, UTF_8);
                msg = null;
            }
            return msgStr;
        }

        @SuppressWarnings("unchecked")
        public <T extends DomainEvent> T getEvent() {
            if (isNull(event)) {
                event = jsonToObject(getMsgStr(), DomainEvent.class);
            }
            return (T) event;
        }
    }

    //消息接收抽象接口
    interface EventReceiver {
        void onMessage(DomainEventMessage message);

        static ReceiveBuilder builder() {
            return new ReceiveBuilder();
        }

        static <T extends DomainEvent> EventReceiver receive(Class<T> cls, Consumer<T> eventConsumer) {
            return builder()
                    .onMessage(cls, eventConsumer)
                    .build();
        }
    }

    //接收具体实现
    @SuppressWarnings("WeakerAccess")
    @Slf4j
    class ReceiveBuilder {
        private List<EventMatcher> matchers = new ArrayList<>();

        public <T extends DomainEvent> ReceiveBuilder onMessage(Class<T> cls, Consumer<T> eventCOnsumer) {
            if (nonNull(cls) && nonNull(eventCOnsumer)) {
                matchers.add(new TypedEventMatcher<>(cls, eventCOnsumer));
            }
            return this;
        }

        public ReceiveBuilder onAnyMessage(Consumer<DomainEvent> consumer) {
            if (nonNull(consumer)){
                matchers.add(new AnyEventMatcher(consumer));
            }
            return this;
        }

        public EventReceiver build() {
            //实现onMessage方法
            return e -> {
                for (EventMatcher m : matchers){
                    if (m.onMessage(e)) {
                        break;
                    }
                }
            };
        }

        private interface EventMatcher {
            boolean onMessage(DomainEventMessage message);
        }

        @Value
        private static class TypedEventMatcher<T extends DomainEvent> implements EventMatcher {
            private Class<T> klass;
            private Consumer<T> consumer;

            @Override
            public boolean onMessage(DomainEventMessage message){
                if (message.isType(klass)) {
                    T e = message.getEvent();
                    log.info("received event:{} {}", message.getDebugMeta(), e);
                    consumer.accept(e);
                    return true;
                }
                return false;
            }
        }

        @Value
        private static class AnyEventMatcher implements EventMatcher {
            private Consumer<DomainEvent> consumer;

            @Override
            public boolean onMessage(DomainEventMessage message) {
                DomainEvent e = message.getEvent();
                log.info("received event:{} {}", message.getDebugMeta(), e);
                consumer.accept(e);
                return true;
            }
        }
    }

    //事件描述类
    class EventSubscriber {
        private ReceiveBuilder builder = new ReceiveBuilder();
        private final DomainEventReceiver receiver;
        private final String serviceName;
        private final String subscribeName;

        EventSubscriber(String serviceName, String subscribeName, DomainEventReceiver receiver) {
            this.receiver = receiver;
            this.serviceName = serviceName;
            this.subscribeName = subscribeName;
        }

        public <T extends DomainEvent> EventSubscriber onMessage(Class<T> cls, Consumer<T> eventConsumer) {
            builder.onMessage(cls, eventConsumer);
            return this;
        }

        public EventSubscriber onAnyMessage(Consumer<DomainEvent> consumer){
            builder.onAnyMessage(consumer);
            return this;
        }

        public void subscribe(){
            EventReceiver build = builder.build();
            receiver.registerCallback(serviceName, subscribeName, build);
        }
    }
}
