package com.redis.demo.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

import static java.util.Objects.isNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author hupeng.net@hotmail.com
 */
public class NamedEnumUtils {

    @Nullable
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E> & NamedEnum> E getEnumById(Integer id, @NotNull E defaultValue) {
        E e = getEnumById(id, (Class<E>) defaultValue.getClass());
        return defaultIfNull(e, defaultValue);
    }

    public static <E extends Enum<E> & NamedEnum> E getEnumById(int id, Class<E> enumType) {
        return EnumSet.allOf(enumType)
            .stream()
            .filter(e -> e.getId() == id)
            .findFirst()
            .orElse(null);
    }

    @Nullable
    public static <E extends Enum<E> & NamedEnum> E getEnumById(Integer id, Class<E> enumType) {
        if (isNull(id)) {
            return null;
        }
        return getEnumById(id.intValue(), enumType);
    }

    public static int getNamedEnumId(NamedEnum e, int defaultValue) {
        if (isNull(e)) {
            return defaultValue;
        }
        return e.getId();
    }

}
