package com.redis.demo.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

import java.io.IOException;

/**
 * @author hupeng.net@hotmail.com
 */
public class NamedEnumDeserializer<E extends Enum<E> & NamedEnum>
    extends JsonDeserializer<NamedEnum>
    implements ContextualDeserializer {

    private Class<E> targetType;
    private final NamedEnumMapper mapper = new NamedEnumMapper();

    @SuppressWarnings("unused")
    public NamedEnumDeserializer() {
        this(null);
    }

    private NamedEnumDeserializer(Class<E> targetType) {
        this.targetType = targetType;
    }

    @Override
    public NamedEnum deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        String value = jp.getText();
        return mapper.toEnum(value, targetType);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        return new NamedEnumDeserializer(
            ctxt.getContextualType().getRawClass()
        );
    }
}
