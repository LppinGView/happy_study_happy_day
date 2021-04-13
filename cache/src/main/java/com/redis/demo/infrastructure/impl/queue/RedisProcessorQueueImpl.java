package com.redis.demo.infrastructure.impl.queue;

import com.redis.demo.domain.service.queue.ProcessorQueue;
import com.redis.demo.domain.service.queue.ProcessorQueueItem;

/**
 * redis优先级队列实现
 */
public class RedisProcessorQueueImpl implements ProcessorQueue<Long> {



    @Override
    public void enqueue(ProcessorQueueItem item) {

    }

    @Override
    public Long getForProcess() {
        return null;
    }

    @Override
    public void markProcessComplete(Long id) {

    }

    @Override
    public Long getQueueRank(Long id) {
        return null;
    }

    @Override
    public void rescheduleTimeoutItem(long timeoutValue) {

    }

    @Override
    public void delayExecuteTask(Long id, long nextExecuteTime) {

    }
}
