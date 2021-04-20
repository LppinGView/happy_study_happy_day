package com.redis.demo.core.event;

import lombok.Getter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@ToString
public class DomainEventWrapper extends ApplicationEvent {
    @Getter
    private List<DomainEvent<?, ?>> events;

    public DomainEventWrapper(Object source, List<DomainEvent<?, ?>> events) {
        super(source);
        this.events = events;
    }

}
