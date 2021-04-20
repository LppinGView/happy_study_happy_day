package com.redis.demo.ddd;

import com.redis.demo.core.event.DomainEvent;
import com.redis.demo.utils.ImpureUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.util.Date;

import static java.util.Objects.nonNull;

/**
 * @author hupeng.net@hotmail.com
 * 注: 子类使用{@link @SuperBuilder}
 * 注: DomainEvent 需要以下注解
 * // @AllArgsConstructor
 * // @NoArgsConstructor(access = PRIVATE, force = true)
 * // @SuperBuilder
 * // @ToString(callSuper = true)
 * // @Value
 * // @EqualsAndHashCode(callSuper = true)
 */

//@SuperBuilder
@SuppressWarnings("unused")
@EqualsAndHashCode
public abstract class DomainEventBase<T, ID> implements DomainEvent<T, ID> {
    @Getter
    protected long eventDate = ImpureUtils.currentTimeMillis();

    @Getter
    @NonNull
    protected String key;

    /**
     * entity id of the event
     */
    @Getter
    protected ID id;

    protected DomainEventBase() {
    }

    @Override
    public String toString() {
        return "{id=" + id + ", eventDate=" + eventDate + ", key=" + key + "}";
    }

    public DomainEventBase(long eventDate, String key, ID id) {
        this.eventDate = eventDate;
        this.key = key;
        this.id = id;
    }

    protected DomainEventBase(DomainEventBaseBuilder<T, ID, ?, ?> b) {
        this.eventDate = b.eventDate;
        this.key = b.key;
        this.id = b.id;
    }

    @Override
    public boolean sameEventAs(T other) {
        return equals(other);
    }

    @SuppressWarnings("WeakerAccess")
    public static abstract class DomainEventBaseBuilder<T, ID, C extends DomainEventBase<T, ID>, B extends DomainEventBaseBuilder<T, ID, C, B>> {
        private long eventDate = ImpureUtils.currentTimeMillis();
        private String key;
        private ID id;

        public B eventDate(long eventDate) {
            this.eventDate = eventDate;
            return self();
        }

        public B eventDate(Date eventDate) {
            if (nonNull(eventDate)) {
                eventDate(eventDate.getTime());
            }
            return self();
        }


        public B key(Object key) {
            if (nonNull(key)) {
                this.key = key.toString();
            }
            return self();
        }

        public B id(ID id) {
            this.id = id;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "DomainEventBaseBuilder(eventDate=" + eventDate + ", key=" + key + ", id=" + id + ")";
        }
    }

}
