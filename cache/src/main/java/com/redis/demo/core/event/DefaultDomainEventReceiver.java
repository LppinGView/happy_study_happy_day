package com.redis.demo.core.event;

import com.redis.demo.utils.ThreadUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.pulsar.client.api.*;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.redis.demo.core.event.Consts.*;
import static com.redis.demo.utils.ExceptionUtil.sneakyInvoke;
import static java.lang.System.getenv;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
public class DefaultDomainEventReceiver implements DomainEventReceiver {

    private final DefaultDomainEventClient client;
    private final String subscribePrefix;
    private final Map<String, ConsumerWrapper> consumerMap = new ConcurrentHashMap<>();

    public DefaultDomainEventReceiver(DefaultDomainEventClient client, String subscribePrefix){
        this.client = client;
        this.subscribePrefix = subscribePrefix;
    }

    //unused?
    @SuppressWarnings("unused")
    private static void accept(String k, ConsumerWrapper v) {
        v.close();
    }

    @Override
    @SneakyThrows
    public void registerCallback(String serviceName, String subscribeName, EventReceiver callback) {
        //设置服务名
        if (isBlank(serviceName)) {
            serviceName = client.getConfig().getServiceName();
        }
        //设置订阅主题
        subscribeName = subscribePrefix + "." + subscribeName;
        String key = serviceName + "|" + subscribeName;
        log.info("register: 事件名和订阅名, eventName:" + serviceName + ", subscribeName:" + subscribeName);
        if (consumerMap.containsKey(key)) {
            consumerMap.get(key).addCallBack(callback);
            return;
        }
        Consumer<byte[]> subscribe = client.getPulsarClient().newConsumer(Schema.BYTES)
                .topicsPattern(Pattern.compile(client.getConfig().getTopicPrefix() + serviceName))
                .subscriptionType(SubscriptionType.Key_Shared)
                .subscriptionName(subscribeName)
                .consumerName(defaultIfBlank(getenv("HOSTNAME"), getenv("HOST")))
                .negativeAckRedeliveryDelay(1, HOURS)
                .subscribe();
        consumerMap.put(key, new ConsumerWrapper(key, subscribe, callback));
    }

    @Override
    public void registerCallback(String subscribeName, EventReceiver callback) {
        registerCallback(client.getConfig().getServiceName(), subscribeName, callback);
    }

    @Override
    public EventSubscriber subscriber(String subscribeName) {
        return subscriber(client.getConfig().getServiceName(), subscribeName);
    }

    //消耗前需要对close资源
    @PreDestroy
    public void dispose() {
        consumerMap.forEach(DefaultDomainEventReceiver::accept);
    }

    class ConsumerWrapper{
        private final Set<EventReceiver> callback = new HashSet<>();
        private final Consumer<byte[]> subscribe;
        private final Thread thread;
        private volatile boolean running = true;

        ConsumerWrapper(String key, Consumer<byte[]> subscribe, EventReceiver callback){
            this.subscribe = subscribe;
            this.callback.add(callback);
            thread = new Thread(() ->{
                ThreadUtils.sleep(30 * 1000);
                log.info("consumer start!");
                while (running && !Thread.currentThread().isInterrupted()){
                    try {
                        Message<byte[]> msg = subscribe.receive();
                        //确保消息接收成功
                        if (this.runWithRetry(msg)) {
                            subscribe.acknowledge(msg);
                        }else {
                            subscribe.negativeAcknowledge(msg);
                        }
                    } catch (Throwable e) {
                        if (e instanceof InterruptedException
                                || e.getCause() instanceof InterruptedException) {
                            break;
                        }
                        log.info("consumer error:", e);
                    }
                }
            });
            thread.setName("ddd-event-consumer-" + key);
            thread.start();
        }

        @SuppressWarnings({"ConstantConditions", "RedundantThrows"})
        private boolean runWithRetry(Message<byte[]> msg) {
            Throwable ex =null;
            for (int i = 0; i < 20; i++) {
                try {
                    notify(msg, i);
                    return true;
                } catch (Throwable e) {
                    if (e instanceof InterruptedException) {
                        subscribe.negativeAcknowledge(msg);
                        throw e;
                    }
                    ex = e;
                    ThreadUtils.sleep(300 * i);
                }
            }
            log.info("msg consume failed!", ex);
            return false;
        }

        void notify(Message<byte[]> msg, int retryIndex) {
            String topic = msg.getTopicName();
            String type = msg.getProperty(MSG_HEADER_MSG_TYPE);
            String source = msg.getProperty(MSG_HEADER_MSG_SOURCE);
            String domainName = msg.getProperty(MSG_HEADER_MSG_DOMAIN_NAME);
            String key = msg.getKey();

            DomainEventMessage event = DomainEventMessage.builder()
                    .topicName(topic)
                    .type(type)
                    .source(source)
                    .msg(msg)
                    .key(key)
                    .domainName(domainName)
                    .build();

            callback.forEach(e -> e.onMessage(event));
        }

        public void addCallBack(EventReceiver callback) {
            this.callback.add(callback);
        }

        //SneakyThrows原理 https://www.jianshu.com/p/7d0ed3aef34b
        @SneakyThrows
        void close(){
            running = false;
            thread.interrupt();
            sneakyInvoke(thread::join);
            sneakyInvoke(subscribe::close);
        }
    }
}
