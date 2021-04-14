package com.redis.demo.utils;

import lombok.NonNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

public class StreamUtil {
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Stream<T> toStream(Optional<List<T>> list) {
        if (nonNull(list) && list.isPresent()) {
            return toStream(list.get());
        }
        return Stream.of();
    }

    public static <T> Stream<T> toStream(List<T> list) {
        return NullableStream.of(list);
    }

    @SafeVarargs
    public static <T> Stream<T> toStream(T... array) {
        return NullableStream.of(array);
    }

    public static <T> Stream<T> toStream(Iterable<T> iterable) {
        return NullableStream.of(iterable);
    }

    public static <T> Stream<T> toStream(Iterator<T> iterator) {
        return NullableStream.of(iterator);
    }

    public static <T, V> Stream<T> toStream(V v, @NonNull Function<V, Iterable<T>> extractor) {
        if (isNull(v)) {
            return Stream.of();
        }
        return toStream(extractor.apply(v));
    }

    public static <K, V> Stream<Map.Entry<K, V>> toStream(Map<K, V> map) {
        return NullableStream.of(map);
    }

    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... a) {
        return toStream(a)
                .filter(Objects::nonNull)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }

    public static <T> Stream<T> concat(List<T> a, List<T> b) {
        return concat(toStream(a), toStream(b));
    }

    public static <T> Stream<T> concat(List<T> a, List<T> b, List<T> c) {
        return concat(toStream(a), toStream(b), toStream(c));
    }

    @SafeVarargs
    public static <T> Stream<T> concat(Optional<List<T>>... a) {
        return toStream(a)
                .filter(Objects::nonNull)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(StreamUtil::toStream)
                .reduce(Stream::concat)
                .orElseGet(Stream::empty);
    }
}
