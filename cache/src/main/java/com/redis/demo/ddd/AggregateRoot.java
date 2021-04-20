package com.redis.demo.ddd;

import com.redis.demo.core.event.DomainEvent;
import com.redis.demo.core.event.DomainEventWrapper;
import org.apache.pulsar.shade.com.google.common.annotations.VisibleForTesting;
import org.apache.pulsar.shade.com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.TestOnly;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.nonNull;


/**
 * 聚合根,
 * 注: 子类使用{@link @SuperBuilder}
 *
 * @param <T>
 * @param <ID>
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class AggregateRoot<T extends Entity<?, ID>, ID> implements Entity<T, ID> {
    @Transient
    protected transient List<DomainEvent<?, ID>> domainEvents = new ArrayList<>();

    //region ddd event sender
    @DomainEvents
    Collection<DomainEventWrapper> domainEvents() {
        return ImmutableList.of(
            new DomainEventWrapper(this, ImmutableList.copyOf(onGetDomainEvents()))
        );
    }

    @AfterDomainEventPublication
    protected void clearDomainEvents() {
        this.onClearDomainEvents();
    }

    protected List<DomainEvent<?,ID>> onGetDomainEvents() {
        return domainEvents;
    }

    protected void onClearDomainEvents() {
        this.domainEvents = new ArrayList<>();
    }
    //endregion


    @VisibleForTesting
    @TestOnly
    List<DomainEvent<?, ID>> getDomainEventsForTest() {
        return ImmutableList.copyOf(domainEvents);
    }

    protected DomainEvent<?, ID> registerEvent(DomainEvent<?, ID> event) {
        this.domainEvents.add(event);
        return event;
    }

    //region AggregateRoot builder
    protected AggregateRoot(AggregateRootBuilder<T, ID, ?, ?> b) {
        this(b.domainEvents);
    }

    public AggregateRoot(List<DomainEvent<?, ID>> domainEvents) {
        if (nonNull(domainEvents)) {
            this.domainEvents.addAll(domainEvents);
        }
    }

    protected AggregateRoot() {
    }

    public static abstract class AggregateRootBuilder<T extends Entity<?, ID>, ID, C extends AggregateRoot<T, ID>, B extends AggregateRootBuilder<T, ID, C, B>> {
        private List<DomainEvent<?, ID>> domainEvents = new ArrayList<>();

        public B registerEvent(DomainEvent<?, ID> domainEvent) {
            this.domainEvents.add(domainEvent);
            return self();
        }

        public B registerEvent(Collection<DomainEvent<?, ID>> domainEvent) {
            this.domainEvents.addAll(domainEvent);
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "AggregateRoot.AggregateRootBuilder(domainEvents=" + this.domainEvents + ")";
        }
    }
    //endregion

}
