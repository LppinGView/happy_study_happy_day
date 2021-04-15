package com.redis.demo.config;

/**
 * @author hupeng.net@hotmail.com
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

//import static com.redis.demo.core.queue.RedisConfigHelper.createConnectionFactory;
import static com.redis.demo.core.queue.RedisConfigHelper.createRedisTemplate;

@Configuration
@ConditionalOnProperty(name = "spring.redis.host")
@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "unchecked"})
public class RedisDefaultConfig {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

//    @Bean
//    public RedisConnectionFactory connectionFactory(RedisProperties properties) {
//        return createConnectionFactory(properties);
//    }

    @Bean
    public StringRedisTemplate redisTemplate(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        return createRedisTemplate(factory);
    }

}
