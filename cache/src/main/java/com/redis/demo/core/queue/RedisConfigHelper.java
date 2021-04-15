package com.redis.demo.core.queue;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static com.redis.demo.utils.ConvertUtils.toSet;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;


/**
 * @author zmh
 */
@SuppressWarnings("unchecked")
public class RedisConfigHelper {

//    public static RedisConnectionFactory createConnectionFactory(RedisProperties properties) {
//        RedisProperties.Cluster cluster = properties.getCluster();
//        RedisProperties.Sentinel sentinel = properties.getSentinel();
//        if (cluster != null && CollectionUtils.isNotEmpty(cluster.getNodes())) {
//            RedisClusterConfiguration config = new RedisClusterConfiguration(cluster.getNodes());
//            Integer maxRedirects = cluster.getMaxRedirects();
//            if (maxRedirects != null) {
//                config.setMaxRedirects(maxRedirects);
//            }
//            if (isNotEmpty(properties.getPassword())) {
//                config.setPassword(RedisPassword.of(properties.getPassword()));
//            }
//            return new JedisConnectionFactory(config);
//        } else if (sentinel != null && isNotEmpty(sentinel.getMaster())) {
//            RedisSentinelConfiguration config = new RedisSentinelConfiguration(sentinel.getMaster(), toSet(sentinel.getNodes()));
//            config.setDatabase(properties.getDatabase());
//            if (isNotEmpty(properties.getPassword())) {
//                config.setPassword(RedisPassword.of(properties.getPassword()));
//            }
//            return new JedisConnectionFactory(config);
//        } else {
//            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//            config.setHostName(properties.getHost());
//            config.setPort(properties.getPort());
//            if (isNotEmpty(properties.getPassword())) {
//                config.setPassword(RedisPassword.of(properties.getPassword()));
//            }
//            config.setDatabase(properties.getDatabase());
//            return new JedisConnectionFactory(config);
//        }
//
//    }

    public static StringRedisTemplate createRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate(factory);
        initKeySerializer(template);
        initValueSerializer(template);
        return template;
    }

    public static void initValueSerializer(RedisTemplate template) {
        Jackson2JsonRedisSerializer serializer = getJackson2JsonRedisSerializer();

        template.setDefaultSerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
    }

    public static void initKeySerializer(RedisTemplate template) {
        StringRedisSerializer serializer = getStringRedisSerializer();

        template.setKeySerializer(serializer);
//        template.setHashKeySerializer(serializer);
    }

    @NotNull
    public static StringRedisSerializer getStringRedisSerializer() {
        return new StringRedisSerializer();
    }

    @NotNull
    public static Jackson2JsonRedisSerializer getJackson2JsonRedisSerializer() {
        // for redis only
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        Jackson2JsonRedisSerializer serializer = new Jackson2JsonRedisSerializer(Object.class);
        serializer.setObjectMapper(objectMapper);
        return serializer;
    }
}
