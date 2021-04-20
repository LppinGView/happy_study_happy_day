package com.redis.demo.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("domain.event")
public class PulsarDomainEventSenderConfig {
    private int bathSize = 100;
    private int maxDelay = 5; // MILLISECONDS
    private int maxPendingMessages = bathSize << 1;
    private int timeout = 10; // SECONDS
    private String topicPrefix = "persistent://public/domain-event/";
    @Value("${spring.application.name:'server'}")
    private String serviceName = "server";

    private Boolean enable = false;
    private String url; // pulsar://10.10.20.65:6650

    public String getTopic() {
        return topicPrefix + serviceName;
    }
}
