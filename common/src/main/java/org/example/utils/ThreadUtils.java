package org.example.utils;

import com.sun.istack.internal.NotNull;

import java.util.concurrent.ThreadFactory;

public class ThreadUtils {

    public static void sleep(int millSeconds){
        try {
            Thread.sleep(millSeconds);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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