package com.redis.demo.ddd;

/**
 * 延迟投递消息
 * 优先级低于 {@link TimeDeliverDomainEvent}
 */
public interface DelayDomainEvent {
    long delayInMilliseconds();
}

