package com.redis.demo;

import com.redis.demo.domain.service.queue.ProcessorQueue;
import com.redis.demo.domain.service.queue.ProcessorQueueItem;
import com.redis.demo.infrastructure.impl.queue.RedisProcessorQueueImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static com.redis.demo.core.queue.PromotionPriorities.PRIORITY_NORMAL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class DemoApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;

    private ProcessorQueue<Long> testQueue = null;

    @Before
    public void init(){
        testQueue = new RedisProcessorQueueImpl("service:test-queue", redisTemplate, 0)
                .resetAll();
    }

    @Test
    public void test_base_function(){

        final long id1 = 1L << 36;
        final long id2 = 2L << 36;

        //测试重复入队请求
        testQueue.enqueue(new ProcessorQueueItem(id1, 3L << 38, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(id1, 3L << 38, PRIORITY_NORMAL));
        Long id = testQueue.getForProcess();
        assertThat(id, equalTo(id1));
        id = testQueue.getForProcess();
        assertThat(id, nullValue());

        testQueue.rescheduleTimeoutItem(10 * 60 * 1000L);
        testQueue.rescheduleTimeoutItem(3);
        testQueue.rescheduleTimeoutItem(3);

        id = testQueue.getForProcess();
        assertThat(id, equalTo(id1));

        testQueue.markProcessComplete(id);
        testQueue.rescheduleTimeoutItem(3);
        testQueue.rescheduleTimeoutItem(3);
        testQueue.rescheduleTimeoutItem(3);

        id = testQueue.getForProcess();
        assertThat(id, nullValue());

        testQueue.enqueue(new ProcessorQueueItem(id1, 5L << 38, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(id2, 3L << 38, PRIORITY_NORMAL));
        id = testQueue.getForProcess();
        assertThat(id, equalTo(id2));

        id = testQueue.getForProcess();
        assertThat(id, equalTo(id1));

        testQueue.rescheduleTimeoutItem(10 * 60 * 1000L);
        assertThat(id, equalTo(id1));
        testQueue.markProcessComplete(id);
    }

}
