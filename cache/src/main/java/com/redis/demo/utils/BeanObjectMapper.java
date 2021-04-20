package com.redis.demo.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author hupeng.net@hotmail.com
 */
@Slf4j
public class BeanObjectMapper extends ObjectMapper {
    private static ObjectMapper current = new ObjectMapper();
    private static final List<Consumer<ObjectMapper>> callbacks = new ArrayList<>();

    public static ObjectMapper create(@NonNull Consumer<ObjectMapper> callback) {
        callbacks.add(callback);
        callback.accept(current);
        return current;
    }

    public static void setObjectMapper(@NonNull ObjectMapper mapper) {
        log.info("global object mapper set!");
        current = mapper;
        callbacks.forEach(c -> c.accept(mapper));
    }

}
