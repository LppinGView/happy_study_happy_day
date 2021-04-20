package com.redis.demo.ddd;

/**
 * 固定时间投递型消息
 * 优先级高于 {@link DelayDomainEvent}
 */
public interface TimeDeliverDomainEvent {
    long deliverAt();
}
