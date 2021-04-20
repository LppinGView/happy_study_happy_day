package com.redis.demo.core.event;

import com.redis.demo.config.PulsarDomainEventSenderConfig;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.pulsar.client.api.PulsarClient;

import static com.redis.demo.utils.ExceptionUtil.sneakyInvoke;

/**
 * 针对pulsar来说，broker是服务端，生成者和消费者都是客户端
 */

public class DefaultDomainEventClient {
    @Getter
    private final PulsarClient pulsarClient;

    @Getter
    private final PulsarDomainEventSenderConfig config;

    @SneakyThrows
    public DefaultDomainEventClient(PulsarDomainEventSenderConfig config){
        this.config = config;
        pulsarClient = PulsarClient.builder()
                .serviceUrl(config.getUrl())
                .enableTcpNoDelay(true)
                .build();
    }

    public void dispose(){
        sneakyInvoke(pulsarClient::close);
    }
}
