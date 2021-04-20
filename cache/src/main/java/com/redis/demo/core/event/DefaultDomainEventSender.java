package com.redis.demo.core.event;

import com.redis.demo.config.PulsarDomainEventSenderConfig;
import com.redis.demo.ddd.AggregateRoot;
import com.redis.demo.ddd.AggregateRootHelper;
import com.redis.demo.ddd.DelayDomainEvent;
import com.redis.demo.ddd.TimeDeliverDomainEvent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;

import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

import static com.redis.demo.core.event.Consts.*;
import static com.redis.demo.utils.ConvertUtils.toJsonString;
import static com.redis.demo.utils.ExceptionUtil.sneakyInvoke;
import static java.lang.System.getenv;
import static java.util.Objects.nonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.pulsar.client.api.CompressionType.ZLIB;
import static org.apache.pulsar.client.api.Schema.STRING;

@Slf4j
public class DefaultDomainEventSender implements DomainEventSender {

    private final Producer<String> producer;

    @SneakyThrows
    public DefaultDomainEventSender(DefaultDomainEventClient client, String prodPrefix){
        PulsarClient pulsarClient = client.getPulsarClient();
        PulsarDomainEventSenderConfig config = client.getConfig();
        producer = pulsarClient
                .newProducer(STRING)
                .topic(config.getTopic())
                .batchingMaxMessages(config.getBathSize())
                .batchingMaxPublishDelay(config.getMaxDelay(), MILLISECONDS)
                .enableBatching(true)
                .blockIfQueueFull(true)
                .maxPendingMessages(config.getMaxPendingMessages())
                .sendTimeout(config.getTimeout(), SECONDS)
                .compressionType(ZLIB)
                .producerName(prodPrefix + "." + defaultIfBlank(getenv("HOSTNAME"), getenv("HOST")))
                .create();
    }

    @Override
    public CompletableFuture<DomainEvent<?, ?>> sendAsync(Object source, DomainEvent<?, ?> event) {
        CompletableFuture<DomainEvent<?, ?>> future = new CompletableFuture<>();
        sendEvent(source, event, future, 5);
        return future;
    }

    private void sendEvent(Object source, DomainEvent<?,?> event, CompletableFuture<DomainEvent<?,?>> result, int retryCount) {
        val msg = producer.newMessage()
                .value(toJsonString(event))
                .eventTime(event.getEventDate())
                .property(MSG_HEADER_MSG_TYPE, event.getClass().getName());

        //从聚合根中获取domain
        String domainName = null;
        if (source instanceof AggregateRoot) {
            domainName = AggregateRootHelper.getDomainName((AggregateRoot<?, ?>) source);
        }

        //从event中获取domain
        if (isBlank(domainName)){
            domainName = AggregateRootHelper.getDomainNameFromEvent(event);
        }

        //配置消息体参数
        if (isNoneBlank(domainName)){
            msg.property(MSG_HEADER_MSG_DOMAIN_NAME, domainName);
        }else{
            domainName = "";
        }

        //实现同一个聚合的事件有序
        String eventKey = null;
        if (isNotBlank(event.getKey())){
            eventKey = event.getKey();
        }else if (nonNull(event.getId())){
            eventKey = event.getId().toString();
            if (isNoneBlank(domainName)){
                eventKey = domainName + ":" + eventKey;
            }
        }
        if (isNoneBlank(eventKey)){
            msg.key(eventKey);
        }

        String eventSource = "";
        if (nonNull(source)){
            eventSource = source.getClass().getName();
            msg.property(MSG_HEADER_MSG_SOURCE, eventSource);
        }

        if (event instanceof TimeDeliverDomainEvent){
            long deliverAt = ((TimeDeliverDomainEvent) event).deliverAt();
            if (deliverAt > 0){
                msg.deliverAt(deliverAt);
            }
        }else if (event instanceof DelayDomainEvent){
            long delay = ((DelayDomainEvent) event).delayInMilliseconds();
            if (delay > 0){
                msg.deliverAfter(delay, MILLISECONDS);
            }
        }

        log.info("sending event: domainName={}, source={}, key={}, {}",domainName, source, eventKey, event);
        msg.sendAsync()
                .thenRun(() -> result.complete(event))
                .exceptionally(ex -> {
                   if (retryCount > 0){
                        log.warn("事件发送失败， 准备重试, retry=" + retryCount + "," + event, ex);
                        sendEvent(source, event, result, retryCount - 1);
                   }else {
                        log.error("事件发送失败:" + event + "," + event, ex);
                        result.completeExceptionally(new DomainEventSendFailedException(ex, event));
                   }
                   return null;
                });
    }

    @PreDestroy
    public void dispose(){
        sneakyInvoke(producer::close);
    }
}
