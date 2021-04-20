package com.redis.demo.utils;

import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class ThreadUtils {

    @SneakyThrows
    public static void sleep(int millSeconds) {
        Thread.sleep(millSeconds);
    }


    public static ThreadFactory namedDaemonThreadFactory(String name) {
        return new ThreadFactory() {
            private volatile int index = 0;

            @Override
            public Thread newThread(@NotNull Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName(name + '-' + index++);
                return t;
            }
        };
    }
}
