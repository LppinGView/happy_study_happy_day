package com.redis.demo.core.stream;

/**
 * @author hupeng.net@hotmail.com
 */
@FunctionalInterface
public interface Action {
    void apply();
}
