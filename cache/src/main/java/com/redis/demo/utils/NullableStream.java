package com.redis.demo.utils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class NullableStream {
    public NullableStream() {
    }

    public static <T> Stream<T> of(List<T> list) {
        return list == null ? Stream.of() : list.stream();
    }

    public static <T> Stream<T> of(T[] array) {
        return array == null ? Stream.of() : Stream.of(array);
    }

    public static <T> Stream<T> of(Iterable<T> iterable) {
        return Objects.isNull(iterable) ? Stream.of() : StreamSupport.stream(iterable.spliterator(), false);
    }

    public static <T> Stream<T> of(Iterator<T> iterator) {
        return Objects.isNull(iterator) ? Stream.of() : StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 16), false);
    }

    public static <K, V> Stream<Map.Entry<K, V>> of(Map<K, V> map) {
        return Objects.isNull(map) ? Stream.of() : of((Iterable)map.entrySet());
    }

    public static <T, R> List<R> mapList(List<T> list, Function<T, R> function) {
        return (List)of(list).map(function).collect(Collectors.toList());
    }

    public static <T, R> List<R> mapList(T[] array, Function<T, R> function) {
        return (List)of(array).map(function).collect(Collectors.toList());
    }
}
