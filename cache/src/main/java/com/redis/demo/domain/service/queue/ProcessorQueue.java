package com.redis.demo.domain.service.queue;

public interface ProcessorQueue<T> {
    //入优先级队列
    void enqueue(ProcessorQueueItem item);

    //优先级最高出队
    T getForProcess();

    //标记处理完成
    void markProcessComplete(T id);

    /**
     * Determine the index of element with value in a sorted set.
     * @param id id in queue
     * @return null: not in queue
     * -1: processing
     * other: order
     */
    Long getQueueRank(T id);

    /**
     * 再次尝试调度
     * @param timeoutValue
     */
    void rescheduleTimeoutItem(long timeoutValue);

    void delayExecuteTask(T id, long nextExecuteTime);
}
