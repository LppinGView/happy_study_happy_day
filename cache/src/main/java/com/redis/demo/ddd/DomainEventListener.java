package com.redis.demo.ddd;


import com.redis.demo.core.event.DomainEvent;

import java.lang.annotation.*;

/**
 * 领域事件监听的订阅
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface DomainEventListener {

    /**
     * 事件消费者的名字
     */
    String subscriberName();

    //  =================== 以下几个条件是and的关系==================

    /**
     * 事件来源的服务, 默认是当前服务
     */
    String server() default "";

    /**
     * 按类型过虑订阅事件
     */
    Class<? extends DomainEvent<?, ?>>[] events() default {};

    String description() default "";

}
