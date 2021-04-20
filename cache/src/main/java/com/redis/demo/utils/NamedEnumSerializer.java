package com.redis.demo.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import java.io.IOException;

import static com.redis.demo.utils.JacksonAnnotationUtils.getAnnotation;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author hupeng.net@hotmail.com
 */
public class NamedEnumSerializer<E extends Enum & NamedEnum>
    extends JsonSerializer<E>
    implements ContextualSerializer {
    private final NamedEnumFormat.FormatStyle formatStyle;

    @SuppressWarnings("unused")
    public NamedEnumSerializer() {
        this(NamedEnumFormat.FormatStyle.STRING);
    }

    private NamedEnumSerializer(NamedEnumFormat.FormatStyle formatStyle) {
        this.formatStyle = formatStyle;
    }

    @Override
    public void serialize(E value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (isNull(value)) {
            return;
        }
        if (NamedEnumFormat.FormatStyle.INTEGER.equals(formatStyle)) {
            gen.writeNumber(value.getId());
        } else {
            gen.writeString(value.name());
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        NamedEnumFormat ann = getAnnotation(property, NamedEnumFormat.class);
        NamedEnumFormat.FormatStyle formatStyle = NamedEnumFormat.FormatStyle.INTEGER;
        if (nonNull(ann)) {
            formatStyle = ann.formatStyle();
        }
        return new NamedEnumSerializer(formatStyle);
    }
}
