package com.redis.demo.core.queue;

import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisSequenceGenerator implements SequenceGenerator<Long>{
    private final BoundHashOperations<String, String, Long> groupSequence;
    private final BoundHashOperations<String, Long, Long> globalSequence;
    private final RedisTemplate redisTemplate;
    private final String queueName;
    private final String KEY_GLOBAL_SEQUENCE = "global-last-sequence";
    private final String KEY_GROUP_SEQUENCE = "group-last-sequence";

    public RedisSequenceGenerator(RedisTemplate redisTemplate, String queueName) {
        this.queueName = queueName;
        this.globalSequence = redisTemplate.boundHashOps(this.parseKey(KEY_GLOBAL_SEQUENCE));
        this.groupSequence = redisTemplate.boundHashOps(this.parseKey(KEY_GROUP_SEQUENCE));
        this.redisTemplate = redisTemplate;
    }

    /**
     * 格式化key
     * @param key
     * @return
     */
    private String parseKey(String key){
        return String.format("%s:%s", this.queueName, key);
    }

    /**
     * 分配序号
     * @param groupId
     * @param priority
     * @return
     */
    @Override
    public Long allocateSequence(long groupId, long priority) {

        return null;
    }

    @Override
    public Long getGlobalSeq(long var1) {
        return null;
    }
}
