package com.redis.demo.core.queue;

/**
 * 序号生成器
 */
interface SequenceGenerator<T> {
    //分配序号
    T allocateSequence(long var1, long var3);

    T getGlobalSeq(long var1);
}
