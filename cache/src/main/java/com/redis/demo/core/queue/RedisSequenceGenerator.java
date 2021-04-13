package com.redis.demo.core.queue;

import com.redis.demo.utils.ConvertUtils;
import org.apache.commons.lang3.ObjectUtils;
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
     * 从全局中获取优先级序号，从组中获取当前序号
     * @param groupId
     * @param priority
     * @return
     */
    @Override
    public Long allocateSequence(long groupId, long priority) {
        String groupSequenceKey = this.getCompanySequenceKey(groupId, priority);
        long globalSeq = this.getGlobalSeq(priority);
        long groupSeq = this.getCompanySeq(groupId, priority);
        long sequence = Math.max(globalSeq, groupSeq) + 1L;
        this.groupSequence.put(groupSequenceKey, sequence);
        return sequence;
    }

    private long getCompanySeq(long groupId, long priority) {
        String groupSequenceKey = this.getCompanySequenceKey(groupId, priority);
        return (Long)ObjectUtils.defaultIfNull(ConvertUtils.toLong((Number) this.groupSequence.get(groupSequenceKey)), 1L);
    }

    private String getCompanySequenceKey(long groupId, long priority) {
        return null;
    }

    @Override
    public Long getGlobalSeq(long priority) {
        return (Long)ObjectUtils.defaultIfNull(ConvertUtils.toLong((Number) this.globalSequence.get(priority)), 1L);
    }
}
