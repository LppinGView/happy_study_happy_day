package com.redis.demo;

import com.redis.demo.ddd.DomainEventBase;
import com.redis.demo.utils.ImpureUtils;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.junit.Assert;
import org.junit.Test;

import static lombok.AccessLevel.PRIVATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class DomainEventBaseTest {

    @Test
    public void builderTest() {
        long eventDate = ImpureUtils.currentTimeMillis();
        TestEvent hi = TestEvent.builder()
            .name("hi")
            .key("key")
            .eventDate(eventDate)
            .build();

        assertEquals("key", hi.getKey());
        Assert.assertTrue(hi.getEventDate() > 0);
        System.out.println(hi.toString());

        TestEvent hi2 = TestEvent.builder()
            .name("hi")
            .key("key")
            .eventDate(eventDate)
            .build();

        TestEvent hi3 = TestEvent.builder()
            .name("hi")
            .key("key2")
            .eventDate(eventDate)
            .build();

        assertEquals(hi, hi2);
        assertNotEquals(hi, hi3);

    }

}

@AllArgsConstructor
@NoArgsConstructor(access = PRIVATE, force = true)
@SuperBuilder
@ToString(callSuper = true)
@Value
@EqualsAndHashCode(callSuper = true)
class TestEvent extends DomainEventBase<TestEvent, Long> {
    private String name;
}
