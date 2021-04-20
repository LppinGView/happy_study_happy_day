package com.redis.demo.utils;

import org.apache.commons.lang3.math.NumberUtils;
import org.mapstruct.TargetType;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author hupeng.net@hotmail.com
 */
public class NamedEnumMapper {

    public <E extends Enum<E> & NamedEnum> E toEnum(String source, @TargetType Class<E> enumType) {
        if (NumberUtils.isCreatable(source)) {
            int id = Integer.parseInt(source);
            return this.toEnum(id, enumType);
        }
        return Enum.valueOf(enumType, source);
    }

    public <E extends Enum<E> & NamedEnum> E toEnum(Number source, @TargetType Class<E> enumType) {
        int id = nonNull(source) ? source.intValue() : -1;
        return NamedEnumUtils.getEnumById(id, enumType);
    }

    public <E extends Enum<E> & NamedEnum> Integer toInteger(E source) {
        if (isNull(source)) {
            return null;
        }
        return source.getId();
    }
}
