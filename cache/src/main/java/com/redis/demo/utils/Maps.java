package com.redis.demo.utils;

import lombok.val;
import org.apache.commons.collections.MapUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.*;

/**
 * @author hupeng.net@hotmail.com
 */
public class Maps {
    private Maps() {
    }

    public static <K, V> MapBuilder<K, V> builder() {
        return builder(HashMap::new);
    }

    public static <K, V> MapBuilder<K, V> builder(Class<K> keyType, Class<V> valueType) {
        return builder(HashMap::new);
    }

    public static <K, V> MapBuilder<K, V> builder(Supplier<Map<K, V>> mapSupplier) {
        return new MapBuilder<>(mapSupplier.get());
    }

    public static <K, V> Map<K, V> singleMap(K key, V value) {
        return Maps.<K, V>builder()
            .put(key, value)
            .build();
    }

    @SafeVarargs
    public static <K, V> Map<K, V> map(Entry<K, V>... entries) {
        val map = new HashMap<K, V>();
        for (Entry<K, V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }

    public static <K, V> Entry<K, V> entry(K key, V value) {
        return new SimpleEntry<>(key, value);
    }

    public static <K, U> Collector<Entry<K, U>, ?, Map<K, U>> toMap() {
        return Collectors.toMap(Entry::getKey, Entry::getValue);
    }

    public static <K, U> Collector<Entry<K, U>, ?, ConcurrentMap<K, U>> toConcurrentMap() {
        return Collectors.toConcurrentMap(Entry::getKey, Entry::getValue);
    }

    public static <K, V> ConcurrentMapBuilder<K, V> concurrentBuilder() {
        return concurrentBuilder(ConcurrentHashMap::new);
    }

    public static <K, V> ConcurrentMapBuilder<K, V> concurrentBuilder(Supplier<ConcurrentMap<K, V>> mapSupplier) {
        return new ConcurrentMapBuilder<>(mapSupplier.get());
    }

    public static <K, V> MultiValueMap<K, V> toMultiValueMap(@Nullable Map<K, V> map) {
        Map<K, List<V>> collect = NullableStream
            .of(map)
            .collect(
                groupingBy(
                    Entry::getKey,
                    mapping(Entry::getValue, toList())
                )
            );
        return new LinkedMultiValueMap<>(collect);
    }

    public static <K, K2, V> Map<K2, V> mapKey(Map<K, V> map, Function<K, K2> mapFun) {
        if (isNull(map)) {
            return null;
        }
        MapBuilder<K2, V> builder = Maps.builder();
        map.forEach((key, value) -> builder.put(mapFun.apply(key), value));
        return builder.build();
    }

    public static <K, V, V2> Map<K, V2> mapValue(Map<K, V> map, Function<V, V2> mapFun) {
        if (isNull(map)) {
            return null;
        }
        MapBuilder<K, V2> builder = Maps.builder();
        map.forEach((key, value) -> builder.put(key, mapFun.apply(value)));
        return builder.build();
    }


    @SuppressWarnings("unchecked")
    public static <K, V, V2> Map<K, V2> toUnmodifiableMap(Map<K, V> map, Function<V, V2> valueMapFun) {
        if (isNull(map)) {
            return null;
        }
        Map<K, V2> map2 = mapValue(map, valueMapFun);
        assert map2 != null;
        return MapUtils.unmodifiableMap(map2);
    }

    public static <K, V, K2, V2> Map<K2, V2> mapKeyValue(
        Map<K, V> map,
        BiFunction<K, V, K2> key,
        BiFunction<K, V, V2> value
    ) {
        if (isNull(map)) {
            return null;
        }
        MapBuilder<K2, V2> builder = Maps.builder();
        map.forEach((k, v) -> builder.put(
            key.apply(k, v),
            value.apply(k, v)
        ));
        return builder.build();
    }

    public static <K, V, R> R computeIfPresent(Map<K, V> map, K key, Function<K, R> apply) {
        if (isNull(map) || map.get(key) == null) {
            return null;
        }
        return apply.apply(key);
    }

    private static class BaseBuilder<M extends Map<K, V>, K, V> {

        protected final M map;

        public BaseBuilder(M map) {
            this.map = map;
        }

        public BaseBuilder<M, K, V> put(K key, V value) {
            if (nonNull(value)) {
                map.put(key, value);
            }
            return this;
        }

        public M build() {
            return map;
        }

    }

    public static class MapBuilder<K, V> extends BaseBuilder<Map<K, V>, K, V> {

        private boolean unmodifiable;

        public MapBuilder(Map<K, V> map) {
            super(map);
        }

        @Override
        public MapBuilder<K, V> put(K key, V value) {
            super.put(key, value);
            return this;
        }

        public MapBuilder<K, V> put(Map<K, V> m) {
            if (nonNull(m)) {
                NullableStream.of(m.entrySet())
                    .forEach(s -> put(s.getKey(), s.getValue()));
            }
            return this;
        }

        public MapBuilder<K, V> unmodifiable(boolean unmodifiable) {
            this.unmodifiable = unmodifiable;
            return this;
        }

        @Override
        public Map<K, V> build() {
            if (unmodifiable) {
                return unmodifiableMap(super.build());
            } else {
                return super.build();
            }
        }

        public MapBuilder<K, V> mapValue(Function<V, V> function) {
            map.entrySet().forEach(s -> {
                K key = s.getKey();
                V value = s.getValue();
                s.setValue(function.apply(value));
            });
            return this;
        }

    }

    public static class ConcurrentMapBuilder<K, V> extends BaseBuilder<ConcurrentMap<K, V>, K, V> {

        public ConcurrentMapBuilder(ConcurrentMap<K, V> map) {
            super(map);
        }

        @Override
        public ConcurrentMapBuilder<K, V> put(K key, V value) {
            super.put(key, value);
            return this;
        }

    }
}
