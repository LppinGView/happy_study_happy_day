package com.redis.demo.utils;

import com.fasterxml.jackson.databind.BeanProperty;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;

import static java.util.Objects.isNull;

/**
 * @author hupeng.net@hotmail.com
 */
public class JacksonAnnotationUtils {

    @Nullable
    public static <A extends Annotation> A getAnnotation(BeanProperty property, Class<A> annClass) {
        if (isNull(property) || isNull(annClass)) {
            return null;
        }
        A ann = property.getAnnotation(annClass);
        if (isNull(ann)) {
            ann = property.getContextAnnotation(annClass);
        }
        return ann;
    }

}
