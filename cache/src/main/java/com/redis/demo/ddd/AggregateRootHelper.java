package com.redis.demo.ddd;

import com.redis.demo.core.event.DomainEvent;
import lombok.val;
import org.apache.pulsar.shade.com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.TestOnly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.*;

public class AggregateRootHelper {

    @TestOnly
    @VisibleForTesting
    public static <ID> List<DomainEvent<?, ID>> getDomainEvents(AggregateRoot<?, ID> entity) {
        return entity.getDomainEventsForTest();
    }

    public static String getDomainName(AggregateRoot<?, ?> aggregateRoot) {
        if (isNull(aggregateRoot)) {
            return null;
        }
        return getDomainName(aggregateRoot.getClass());
    }

    private static volatile Map<Class<?>, String> domainNameCache = new HashMap<>();

    public static <T extends DomainEvent<?, ?>> String getDomainNameFromEvent(T event) {
        if (isNull(event)) {
            return null;
        }
        return getDomainNameFromEvent(event.getClass());
    }

    public static <T extends DomainEvent<?, ?>> String getDomainNameFromEvent(Class<T> klass) {
        if (domainNameCache.containsKey(klass)) {
            return domainNameCache.get(klass);
        }
        synchronized (AggregateRootHelper.class) {
            String name = domainNameCache.get(klass);
            if (isNull(name)) {
                name = getDomainNameFromAnnotation(klass);
                val newMap = new HashMap<>(domainNameCache);
                newMap.put(klass, name);
                domainNameCache = newMap;
            }
            return name;
        }
    }

    public static <T extends AggregateRoot<?, ?>> String getDomainName(Class<T> klass) {
        String name = domainNameCache.get(klass);
        if (isNull(name)) {
            synchronized (AggregateRootHelper.class) {
                name = domainNameCache.get(klass);
                if (isNull(name)) {
                    name = getDomainNameInner(klass);
                    val newMap = new HashMap<>(domainNameCache);
                    newMap.put(klass, name);
                    domainNameCache = newMap;
                }
            }
        }
        return name;
    }

    static <T extends AggregateRoot<?, ?>> String getDomainNameInner(Class<T> klass) {
        String name = getDomainNameFromAnnotation(klass);
        if (isNotBlank(name)) {
            return name;
        }

        name = klass.getName();
        String[] nameParts = split(name, '.');

        StringBuilder sb = new StringBuilder();
        if (nameParts.length > 1) {
            for (int i = 0; i < nameParts.length - 1; i++) {
                sb.append((char) (nameParts[i].charAt(0) | 0x20));
            }
            sb.append('-').append(nameParts[nameParts.length - 1]);
            name = sb.toString();
        }
        return replaceChars(name, '$', '-');
    }

    static <T> String getDomainNameFromAnnotation(Class<T> klass) {
        String name = null;
        DomainAggregateRootName annotation = klass.getAnnotation(DomainAggregateRootName.class);
        if (nonNull(annotation)) {
            name = annotation.value();
        }
        return name;
    }

}
