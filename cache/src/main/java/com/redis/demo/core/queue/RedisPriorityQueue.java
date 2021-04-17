package com.redis.demo.core.queue;

import com.redis.demo.utils.CollectionUtils;
import com.redis.demo.utils.CompareUtils;
import com.redis.demo.utils.ConvertUtils;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.connection.RedisZSetCommands.Range;
import org.springframework.data.redis.core.BoundZSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Arrays;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.redis.demo.utils.Maps.entry;
import static com.redis.demo.utils.StreamUtil.toStream;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@SuppressWarnings("WeakerAccess")
public class RedisPriorityQueue {
    private static final Logger log = LoggerFactory.getLogger(RedisPriorityQueue.class);
    private final BoundZSetOperations<String, Long> priorityQueue;
    private final BoundZSetOperations<String, Long> processingQueue;
    private final BoundZSetOperations<String, Long> processingQueueMonitor;
    private final String KEY_PRIORITY_QUEUE = "priority-queue";
    private final String KEY_PROCESSING_QUEUE = "processing-queue";
    private final String KEY_PROCESSING_MONITOR_QUEUE = "processing-queue-monitor";
    private final RedisTemplate redisTemplate;
    private final String queueName;
    private int lockTimeOutInSecond = 0;
    private long processTimeOutInMillSecond = 10 * 60 * 1000L; //10分钟

    @SuppressWarnings("unchecked")
    public RedisPriorityQueue(String queueName, RedisTemplate redisTemplate) {
        this.queueName = queueName;
        this.priorityQueue = redisTemplate.boundZSetOps(this.parseKey(KEY_PRIORITY_QUEUE));
        this.processingQueue = redisTemplate.boundZSetOps(this.parseKey(KEY_PROCESSING_QUEUE));
        this.processingQueueMonitor = redisTemplate.boundZSetOps(this.parseKey(KEY_PROCESSING_MONITOR_QUEUE));
        this.redisTemplate = redisTemplate;
    }

    private String parseKey(String key){
        return String.format("%s:%s", this.queueName, key);
    }

    @SuppressWarnings({"unchecked", "Convert2MethodRef"})
    public void resetAll(){
        this.peekTimeout(0);
        this.processTimeout(600);
        this.redisTemplate.delete(Arrays.asList(
                this.parseKey(KEY_PRIORITY_QUEUE),
                this.parseKey(KEY_PROCESSING_QUEUE),
                this.parseKey(KEY_PROCESSING_MONITOR_QUEUE)
        ));
    }

    public RedisPriorityQueue peekTimeout(int timeoutInSecond){
        this.lockTimeOutInSecond = timeoutInSecond;
        return this;
    }

    public RedisPriorityQueue processTimeout(int timeoutInSecond){
        this.processTimeOutInMillSecond = TimeUnit.SECONDS.toMillis((long)timeoutInSecond);
        return this;
    }

    public void enqueue(long id, long score){
        this.priorityQueue.add(id, (double)score);
    }

    public Long getScore(long id){
        Double score = this.priorityQueue.score(id);
        return Objects.nonNull(score) ? score.longValue() : null;
    }

    @SneakyThrows
    private Long peekTopBlocked() {
        try {
            long timeout = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis((long) this.lockTimeOutInSecond);
            long sleepLimit = 128L;

            //在超时范围内允许重试
            do {
                Long id = this.peekTop();
                if (Objects.nonNull(id)){
                    return id;
                }

                //随机睡一段时间再重试
                if(this.lockTimeOutInSecond > 0){
                    //乘以2
                    sleepLimit <<= 1;
                    if (sleepLimit > 1000L){
                        sleepLimit = 1000L;
                    }

                    Thread.sleep(RandomUtils.nextLong(128L, sleepLimit));
                }

            }while (System.currentTimeMillis() < timeout);

            return null;
        }catch (Throwable var6){
            throw var6;
        }
    }

    public Long peekTop(){
        Set<Long> set = this.priorityQueue.rangeByLex(Range.unbounded(), Limit.limit().count(1));
        Number id = CollectionUtils.firstOrNull(set); // redis 返回值可能是Integer
        return ConvertUtils.toLong(id);
    }

    public void markProcessComplete(long id){
        this.processingQueueMonitor.remove(new Object[]{id});
        this.processingQueue.remove(new Object[]{id});
    }

    public Long getRank(long id){
        Long rank = this.priorityQueue.rank(id);
        if (Objects.nonNull(rank)){
            return rank;
        }else {
            rank = this.processingQueue.rank(id);
            return Objects.nonNull(rank) ? -1L : null;
        }
    }

    /**
     * 将待处理事务从优先级队列移除，同时放进processing队列和监控队列
     * @param id
     * @param sorce
     * @return true,表示移除成功
     */
    public boolean transGetIdForProcess(Long id, Long sorce){
        log.debug("transGetIdForProcess, {}, {}", id, sorce);
        this.processingQueue.add(id, sorce);
        this.processingQueueMonitor.add(id, System.currentTimeMillis() + this.processTimeOutInMillSecond);
        Long removeResult = this.priorityQueue.remove(new Object[]{id});
        return CompareUtils.areEquals(1L, removeResult);
    }

    public Long getForProcess(){
        Entry<Long, Long> entry = this.getIdScopeForProcess();
        return null != entry ? (Long)entry.getKey() : null;
    }

    public Entry<Long, Long> getIdScopeForProcess() {
        //阻塞式获取优先级队列队首元素
        Long id = this.peekTopBlocked();

        if (isNull(id)){
            return null;
        }

        Long score = this.getScore(id);
        if (isNull(score)){
            return null;
        }

        log.debug("try get id for process, {}, {}", id, score);
        boolean success = this.transGetIdForProcess(id, score);
        log.debug("get id={}, success={}", id, success);
        return success ? entry(id, score) : null;
    }

    public void rescheduleTimeoutItem(){
        rescheduleTimeoutItem(System.currentTimeMillis());
    }

    /**
     * 对某个时间段内的任务再次调度 进行处理
     * 将监控队列中的任务，score置为-1，同时将其移除处理队里，再将其加入到优先级队列中，同时移除监控队列score=-1的任务
     * @param timeoutValue
     */
    public synchronized void rescheduleTimeoutItem(long timeoutValue){
        Set<Long> set = processingQueueMonitor.rangeByScore(Double.MIN_VALUE, (double)(System.currentTimeMillis() + timeoutValue));
        toStream(set)
            .map(ConvertUtils::toLong)
            .filter(Objects::nonNull)
            .forEach(id ->{
                Double score = processingQueue.score(id);
                if (isNull(score)){
                    score = 0d;
                }
                processingQueueMonitor.add(id, -1);
                processingQueue.remove(id);
                priorityQueue.add(id, score);
                processingQueueMonitor.removeRangeByScore(-1, -1);
            });
    }

    public synchronized void setNextScheduleTime(long id, long nextScheduleTime){
        if (nonNull(processingQueueMonitor.score(id))){
            processingQueueMonitor.add(id, nextScheduleTime);
        }
    }
}
