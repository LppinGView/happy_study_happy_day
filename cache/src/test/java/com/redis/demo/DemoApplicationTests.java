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

import static com.redis.demo.core.queue.PromotionPriorities.*;
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

    @Test
    public void test_priority_withManyCompany() {
        //同一个公司，同优先级，依次入队，依次出队
        testQueue.enqueue(new ProcessorQueueItem(1L, 3L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(2L, 3L, PRIORITY_NORMAL));
        assertThat(testQueue.getForProcess(), equalTo(1L));
        assertThat(testQueue.getForProcess(), equalTo(2L));
        testQueue.markProcessComplete(1L);
        testQueue.markProcessComplete(2L);

        //同一个公司的，高优先级的，优先出队
        testQueue.enqueue(new ProcessorQueueItem(1L, 3L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(2L, 3L, PRIORITY_HIGH));
        assertThat(testQueue.getForProcess(), equalTo(2L));
        assertThat(testQueue.getForProcess(), equalTo(1L));
        testQueue.markProcessComplete(1L);
        testQueue.markProcessComplete(2L);

        //多个公司，同一个优先级，依次出队
        testQueue.enqueue(new ProcessorQueueItem(1L, 3L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(2L, 3L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(3L, 4L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(4L, 4L, PRIORITY_NORMAL));
        assertThat(testQueue.getForProcess(), equalTo(1L));
        assertThat(testQueue.getForProcess(), equalTo(3L));
        assertThat(testQueue.getForProcess(), equalTo(2L));
        assertThat(testQueue.getForProcess(), equalTo(4L));
        testQueue.markProcessComplete(1L);
        testQueue.markProcessComplete(2L);
        testQueue.markProcessComplete(3L);
        testQueue.markProcessComplete(4L);

        //优先级为主
        testQueue.enqueue(new ProcessorQueueItem(1L, 3L, PRIORITY_LOW));
        testQueue.enqueue(new ProcessorQueueItem(2L, 3L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(3L, 4L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(4L, 4L, PRIORITY_HIGH));
        assertThat(testQueue.getForProcess(), equalTo(4L));
        assertThat(testQueue.getForProcess(), equalTo(2L));
        assertThat(testQueue.getForProcess(), equalTo(3L));
        assertThat(testQueue.getForProcess(), equalTo(1L));
        testQueue.markProcessComplete(1L);
        testQueue.markProcessComplete(2L);
        testQueue.markProcessComplete(3L);
        testQueue.markProcessComplete(4L);
    }

//    @Test
//    public void test_priority_withManyCompany2() {
//        testQueue.enqueue(new ProcessorQueueItem(1L<<38, 3L, PRIORITY_LOW));
//        testQueue.enqueue(new ProcessorQueueItem(2L<<38, 3L, PRIORITY_NORMAL));
//        assertThat(testQueue.getForProcess(), equalTo(2L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(1L<<38));
//        // global to 3
//        testQueue.enqueue(new ProcessorQueueItem(3L<<38, 4L, PRIORITY_LOW));
//        testQueue.enqueue(new ProcessorQueueItem(4L<<38, 3L, PRIORITY_NORMAL));
//        testQueue.enqueue(new ProcessorQueueItem(5L<<38, 4L, PRIORITY_NORMAL));
//        testQueue.enqueue(new ProcessorQueueItem(6L<<38, 4L, PRIORITY_HIGH));
//        testQueue.enqueue(new ProcessorQueueItem(7L<<38, 3L, PRIORITY_HIGH));
//        assertThat(testQueue.getForProcess(), equalTo(6L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(7L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(4L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(5L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(3L<<38));
//        testQueue.markProcessComplete(1L<<38);
//        testQueue.markProcessComplete(5L<<38);
//        testQueue.markProcessComplete(7L<<38);
//
//        testQueue.rescheduleTimeoutItem(System.currentTimeMillis());
//        testQueue.rescheduleTimeoutItem(System.currentTimeMillis());
//        testQueue.rescheduleTimeoutItem(System.currentTimeMillis());
//
//        assertThat(testQueue.getQueueRank(3L<<38), equalTo(3L));
//        assertThat(testQueue.getQueueRank(2L<<38), equalTo(1L));
//
//        assertThat(testQueue.getForProcess(), equalTo(6L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(2L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(4L<<38));
//        assertThat(testQueue.getForProcess(), equalTo(3L<<38));
//    }

    @Test
    public void test_priority_withManyCompany2() {
        testQueue.enqueue(new ProcessorQueueItem(1L, 3L, PRIORITY_LOW));
        testQueue.enqueue(new ProcessorQueueItem(2L, 3L, PRIORITY_NORMAL));
        assertThat(testQueue.getForProcess(), equalTo(2L));
        assertThat(testQueue.getForProcess(), equalTo(1L));
        // global to 3
        testQueue.enqueue(new ProcessorQueueItem(3L, 4L, PRIORITY_LOW));
        testQueue.enqueue(new ProcessorQueueItem(4L, 3L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(5L, 4L, PRIORITY_NORMAL));
        testQueue.enqueue(new ProcessorQueueItem(6L, 4L, PRIORITY_HIGH));
        testQueue.enqueue(new ProcessorQueueItem(7L, 3L, PRIORITY_HIGH));
        assertThat(testQueue.getForProcess(), equalTo(6L));
        assertThat(testQueue.getForProcess(), equalTo(7L));
        assertThat(testQueue.getForProcess(), equalTo(4L));
        assertThat(testQueue.getForProcess(), equalTo(5L));
        assertThat(testQueue.getForProcess(), equalTo(3L));
        //queue=[]
        //processing=[6,7,2,4,5,1,3]
        //monitor=[6,7,2,4,5,1,3]
        testQueue.markProcessComplete(1L);
        testQueue.markProcessComplete(5L);
        testQueue.markProcessComplete(7L);

        //processing=[6,2,4,3]
        //monitor=[6,2,4,3]
        //重复调度，会进行覆盖
        testQueue.rescheduleTimeoutItem(System.currentTimeMillis());
        testQueue.rescheduleTimeoutItem(System.currentTimeMillis());
        testQueue.rescheduleTimeoutItem(System.currentTimeMillis());

        //queue=[6,2,4,3]
        //processing=[]
        //monitor=[]
        assertThat(testQueue.getQueueRank(3L), equalTo(3L));
        assertThat(testQueue.getQueueRank(2L), equalTo(1L));
        assertThat(testQueue.getQueueRank(6L), equalTo(0L));

        assertThat(testQueue.getForProcess(), equalTo(6L));
        assertThat(testQueue.getForProcess(), equalTo(2L));
        assertThat(testQueue.getForProcess(), equalTo(4L));
        assertThat(testQueue.getForProcess(), equalTo(3L));
    }
}
