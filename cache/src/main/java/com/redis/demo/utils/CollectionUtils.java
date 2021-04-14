package com.redis.demo.utils;

import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.redis.demo.utils.StreamUtil.toStream;
import static java.lang.reflect.Array.newInstance;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;

public class CollectionUtils {
    public static <T> T firstOrNull(Iterable<T> collection) {
        return firstOrDefault(collection, null);
    }

    public static <T> T firstOrDefault(Iterable<T> collection, T defaultValue) {
        if (collection != null) {
            Iterator<T> iterator = collection.iterator();
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return defaultValue;
    }

    public static <T> T firstOrDefault(T[] collection, T defaultValue) {
        if (ArrayUtils.isEmpty(collection)) {
            return defaultValue;
        }
        return collection[0];
    }

    public static <T> T last(Iterable<T> collection) {
        return toStream(collection)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    public static <T> List<T> distinct(List<T> list) {
        return NullableStream
                .of(list)
                .distinct()
                .collect(Collectors.toList());
    }

    public static <T> List<T> addItems(List<T> list, T... items) {
        if (isNull(list)) {
            list = new ArrayList<>();
        }
        list.addAll(asList(items));
        return list;
    }

    public static String[] toArray(Collection<String> array) {
        return toArray(array, String.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Collection<T> array, Class<T> klass) {
        if (isNull(array)) {
            return null;
        }
        T[] result = (T[]) newInstance(klass, array.size());
        return array.toArray(result);
    }

    public static <T> List<T> defaultIfEmpty(List<T> list, T... items) {
        if (org.apache.commons.collections.CollectionUtils.isEmpty(list)) {
            return asList(items);
        }
        return list;
    }

    public static <T> void forEach(Iterable<T> iterable, Consumer<T> fun) {
        toStream(iterable).forEach(fun);
    }

    public static <T> void forEach(T[] array, Consumer<T> fun) {
        toStream(array).forEach(fun);
    }

    public static <K, V> void forEach(Map<K, V> map, Consumer<Map.Entry<K, V>> fun) {
        toStream(map).forEach(fun);
    }
}
