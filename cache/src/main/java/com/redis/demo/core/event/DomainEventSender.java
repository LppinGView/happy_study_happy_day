package com.redis.demo.core.event;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.redis.demo.utils.ExceptionUtil.sneakyCatch;
import static com.redis.demo.utils.StreamUtil.toStream;
import static java.util.Objects.isNull;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public interface DomainEventSender {
    default void send(Object source, List<DomainEvent<?, ?>> eventList) throws DomainEventSendFailedException {
        if (isNull(eventList)) {
            return;
        }
        toStream(eventList)
                .filter(Objects::nonNull)
                .map(event -> sendAsync(source, event))
                .forEach(sneakyCatch(result -> {
                    //noinspection Convert2MethodRef
                    result.get();
                }));
    }

    default void send(DomainEvent<?, ?>... events) throws DomainEventSendFailedException {
        send(null, Arrays.asList(events));
    }

    default List<CompletableFuture<DomainEvent<?, ?>>> sendAsync(Object source, List<DomainEvent<?, ?>> eventList) {
        return toStream(eventList)
                .filter(Objects::nonNull)
                .map(event -> sendAsync(source, event))
                .collect(toList());
    }
    /**
     * @throws DomainEventSendFailedException
     */
    @SuppressWarnings("JavaDoc")
    CompletableFuture<DomainEvent<?, ?>> sendAsync(Object source, DomainEvent<?, ?> event);

    class DomainEventSendFailedException extends RuntimeException {
        @Getter
        private final DomainEvent<?, ?> event;

        public DomainEventSendFailedException(Throwable cause, DomainEvent<?, ?> event) {
            super(cause.getMessage(), cause);
            this.event = event;
        }
    }
}
