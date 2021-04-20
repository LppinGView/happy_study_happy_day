package com.redis.demo.utils;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.Validate.notNull;
import static org.springframework.core.ResolvableType.forClassWithGenerics;

public final class ConvertUtils {
    private static final Logger log = LoggerFactory.getLogger(ConvertUtils.class);
    @SuppressWarnings("unchecked")
    private static final Class<Map<String, Object>> MAP_TYPE = (Class<Map<String, Object>>)
            notNull(forClassWithGenerics(Map.class, String.class, Object.class).resolve());

    //region register modules
    private static final List<Module> registerModules = new ArrayList<>(singletonList(new NamedEnumModule()));

    private static ObjectMapper OBJECTMAPPER = BeanObjectMapper.create(m -> ConvertUtils.OBJECTMAPPER = applyModules(m));
    private static ObjectMapper TYPEINFO_OBJECTMAPPER = new ObjectMapper()
            .enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

    public ConvertUtils() {
    }

    public static Integer toInteger(Object value) {
        return toInteger(value, (Integer)null);
    }

    public static Integer toInteger(Object value, Integer defaultValue) {
        if (Objects.isNull(value)) {
            return defaultValue;
        } else if (value instanceof Number) {
            return ((Number)value).intValue();
        } else {
            return value instanceof String ? Integer.valueOf((String)value) : defaultValue;
        }
    }

    public static int unboxInteger(Number n, int defaultValue) {
        return Objects.isNull(n) ? defaultValue : n.intValue();
    }

    public static long unboxLong(Number n, long defaultValue) {
        return Objects.isNull(n) ? defaultValue : n.longValue();
    }

    public static Number toNumber(Object number) {
        return (Number) number;
    }

    public static Long toLong(Number number) {
        return Objects.isNull(number) ? null : number.longValue();
    }

    public static Long toLong(Object number) {
        return toLong(number, (Long)null);
    }

    public static String toNormalString(Object obj) {
        return Objects.isNull(obj) ? "" : obj.toString();
    }

    public static Long toLong(Object number, Long defaultValue) {
        if (Objects.isNull(number)) {
            return null;
        } else if (number instanceof Number) {
            return ((Number)number).longValue();
        } else {
            return number instanceof String ? Long.valueOf((String)number) : defaultValue;
        }
    }

    public static BigDecimal toBigDecimal(Integer value) {
        return Objects.isNull(value) ? null : BigDecimal.valueOf((long)value);
    }

//    public static Map<String, Object> toMap(Object object) {
//        return (Map)(Objects.isNull(object) ? new HashMap() : (Map)OBJECTMAPPER.convertValue(object, MAP_TYPE));
//    }
//
//    public static <T> T convertObject(Object object, Class<T> cls) {
//        return Objects.isNull(object) ? null : OBJECTMAPPER.convertValue(object, cls);
//    }
//
//    public static void registerModule(@NonNull Module module) {
//        if (module == null) {
//            throw new NullPointerException("module is marked non-null but is null");
//        } else if (!registerModules.contains(module)) {
//            registerModules.add(module);
//            OBJECTMAPPER.registerModule(module);
//            TYPEINFO_OBJECTMAPPER.registerModule(module);
//        }
//    }
//
    private static ObjectMapper applyModules(ObjectMapper mapper) {
        registerModules.forEach(mapper::registerModule);
        mapper
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .enable(ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        ;

        return mapper;
    }
//
//    public static JsonNode toJsonNode(@Language("JSON") String json) {
//        try {
//            return Objects.isNull(json) ? null : (JsonNode)OBJECTMAPPER.readValue(json, JsonNode.class);
//        } catch (IOException var2) {
//            throw var2;
//        }
//    }
//
//    public static JsonNode toJsonNode(Object object) {
//        return Objects.isNull(object) ? null : OBJECTMAPPER.valueToTree(object);
//    }
//
//    public static String toJsonString(Object object) {
//        try {
//            return OBJECTMAPPER.writeValueAsString(object);
//        } catch (JsonProcessingException var2) {
//            throw var2;
//        }
//    }
//
//    public static String toJsonStringWithTypeInfo(Object object) {
//        try {
//            return TYPEINFO_OBJECTMAPPER.writeValueAsString(object);
//        } catch (JsonProcessingException var2) {
//            throw var2;
//        }
//    }
    @SneakyThrows(JsonProcessingException.class)
    public static String toJsonString(Object object) {
        return OBJECTMAPPER.writeValueAsString(object);
    }
//
//    public static Map<String, Object> jsonToMap(String json) {
//        try {
//            return (Map)(StringUtils.isBlank(json) ? new HashMap() : (Map)OBJECTMAPPER.readValue(json, MAP_TYPE));
//        } catch (IOException var2) {
//            throw var2;
//        }
//    }

    @SneakyThrows(IOException.class)
    public static <T> T jsonToObject(String json, Class<T> klass) {
        return StringUtils.isBlank(json) ? null : OBJECTMAPPER.readValue(json, klass);
    }

//    public static <T> T jsonWithTypeInfoToObject(String json) {
//        try {
//            return StringUtils.isBlank(json) ? null : TYPEINFO_OBJECTMAPPER.readValue(json, Object.class);
//        } catch (IOException var2) {
//            throw var2;
//        }
//    }
//
//    public static <T> T jsonWithTypeInfoToObject(String json, Class<T> klass) {
//        try {
//            return StringUtils.isBlank(json) ? null : TYPEINFO_OBJECTMAPPER.readValue(json, klass);
//        } catch (IOException var3) {
//            throw var3;
//        }
//    }

    @SneakyThrows(IOException.class)
    public static <T> T jsonToObject(JsonNode json, Class<T> klass) {
        return Objects.isNull(json) ? null : OBJECTMAPPER.treeToValue(json, klass);
    }
//
//    @NotNull
//    public static List<String> splitToList(String str, char splitChar) {
//        String[] split = StringUtils.split(str, splitChar);
//        return Objects.isNull(split) ? ListUtils.EMPTY_LIST : Arrays.asList(split);
//    }
//
//    public static String toSnakeCase(String str) {
//        return StringUtils.isBlank(str) ? str : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, str);
//    }
//
//    public static <T> T clone(T value) {
//        return KryoUtil.kryo((k) -> {
//            return k.copy(value);
//        });
//    }
//
//    public static <T> T deepCopy(T value) {
//        return customSerializer.deepCopy(value);
//    }
//
    public static <T> Set<T> toSet(Iterable<T> nodes) {
        return (Set)StreamUtil.toStream(nodes).collect(Collectors.toSet());
    }
//
//    static {
//        TYPEINFO_OBJECTMAPPER = (new ObjectMapper()).enableDefaultTyping(DefaultTyping.NON_FINAL, As.PROPERTY);
//        customSerializer = new CustomSerializer();
//    }
//
//    @JsonTypeInfo(
//            use = Id.CLASS
//    )
//    public interface WithTypeInfoJson {
//    }
}
