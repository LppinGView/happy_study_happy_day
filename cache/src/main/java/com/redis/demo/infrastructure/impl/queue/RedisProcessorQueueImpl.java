package com.redis.demo.infrastructure.impl.queue;

import com.redis.demo.core.queue.RedisPriorityQueue;
import com.redis.demo.core.queue.RedisSequenceGenerator;
import com.redis.demo.domain.service.queue.ProcessorQueue;
import com.redis.demo.domain.service.queue.ProcessorQueueItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static java.lang.Math.max;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * redis优先级队列实现
 */
@Slf4j
public class RedisProcessorQueueImpl implements ProcessorQueue<Long> {

    private final RedisSequenceGenerator sequenceGenerator;
    private final RedisPriorityQueue priorityQueue;

    public RedisProcessorQueueImpl(String queueName, RedisTemplate redisTemplate, int lockTimeoutInSecond) {
        this.sequenceGenerator = new RedisSequenceGenerator(queueName, redisTemplate);
        this.priorityQueue = new RedisPriorityQueue(queueName, redisTemplate);
        this.priorityQueue.peekTimeout(lockTimeoutInSecond);
    }

    //for test only
    RedisProcessorQueueImpl resetAll(){
        priorityQueue.resetAll();
        sequenceGenerator.resetAll();
        return this;
    }

    @Override
    public void enqueue(ProcessorQueueItem item) {
        final long id = item.getId();
        final long groupId = item.getCompanyId();
        final long priority = item.getPriority();

        Long prevScore = priorityQueue.getScore(id);
        long sequence;
        if(nonNull(prevScore)){ //exists, do NOT change queue order
            sequence = QueueUtils.scoreToSequence(prevScore);
        }else {
            sequence = sequenceGenerator.allocateSequence(groupId, priority);
        }
        final long scroe = QueueUtils.sequenceToScore(sequence, priority);
        log.debug("seq={}, score={}, id={}, company={}, priority={}", sequence, scroe, item.getId(), item.getCompanyId(), item.getPriority());
        priorityQueue.enqueue(id, scroe);
    }

    /**
     * 从队列中获取优先级最高的进行处理，同时将其移除队列，并加入处理队列和监控队列
     * @return
     */
    @Override
    public Long getForProcess() {
        Map.Entry<Long, Long> id = priorityQueue.getIdScopeForProcess();
        if (isNull(id)){
            return null;
        }

        final Long score = id.getValue();
        long priority = QueueUtils.scoreToPriority(score);
        long sequence = calcNextGlobalSequence(score);
        sequenceGenerator.transUpdateGlobalSequence(priority, sequence);
        return id.getKey();
    }

    public long calcNextGlobalSequence(long score){
        long priority = QueueUtils.scoreToPriority(score);
        long itemSequence = QueueUtils.scoreToSequence(score);
        long globalSeq = sequenceGenerator.getGlobalSeq(priority);
        return max(itemSequence, globalSeq);
    }

    @Override
    public void markProcessComplete(Long id) {
        priorityQueue.markProcessComplete(id);
    }

    @Override
    public Long getQueueRank(Long id) {
        return priorityQueue.getRank(id);
    }

    @Override
    public void rescheduleTimeoutItem(long timeoutValue) {
        priorityQueue.rescheduleTimeoutItem(timeoutValue);
    }

    @Override
    public void delayExecuteTask(Long id, long nextExecuteTime) {
        priorityQueue.setNextScheduleTime(id, nextExecuteTime);
    }
}
