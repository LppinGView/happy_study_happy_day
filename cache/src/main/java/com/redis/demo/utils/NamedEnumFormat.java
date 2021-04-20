package com.redis.demo.utils;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

import java.lang.annotation.*;

/**
 * @author hupeng.net@hotmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@JacksonAnnotationsInside
@Documented
public @interface NamedEnumFormat {

    FormatStyle formatStyle() default FormatStyle.INTEGER;

    enum FormatStyle {
        INTEGER, STRING
    }
}
