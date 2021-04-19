package com.redis.demo.core.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;

/**
 * @author hupeng.net@hotmail.com
 * 注: 需要以下注解
 * // @AllArgsConstructor
 * // @NoArgsConstructor(access = PRIVATE, force = true)
 * // @SuperBuilder
 * // @ToString(callSuper = true)
 * // @Value
 * // @EqualsAndHashCode(callSuper = true)
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "type")
public interface DomainEvent<T, ID> extends Serializable {

    //消息是否相同
    boolean sameEventAs(T other);

    //获取消息时间
    long getEventDate();

    //获取消息key
    String getKey();

    //获取消息Id
    default ID getId() {
        return null;
    }

}

