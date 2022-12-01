package com.lpp.demo.copyZoo.server;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkerService {

    //工作线程池数目
    private final ArrayList<ExecutorService> workers = new ArrayList<ExecutorService>();

    private final String threadNamePrefix;
    private int numWorkerThreads;
    private boolean threadsAreAssignable; //是不是分配线程，不分配，则创建单个 n线程的线程池

    public WorkerService(String name, int numThreads, boolean useAssignableThreads) {
        this.threadNamePrefix = (name == null ? "" : name) + "Thread";
        this.numWorkerThreads = numThreads;
        this.threadsAreAssignable = useAssignableThreads;
        start();
    }

    public void start() {
        if (numWorkerThreads > 0) {
            if (threadsAreAssignable) {
                for (int i = 1; i <= numWorkerThreads; i++) {
                    workers.add(Executors.newFixedThreadPool(1, new DaemonThreadFactory(threadNamePrefix, i)));
                }
            } else {
                workers.add(Executors.newFixedThreadPool(numWorkerThreads, new DaemonThreadFactory(threadNamePrefix)));
            }
        }
    }

    public void schedule(WorkRequest workRequest) {
        schedule(workRequest, 0);
    }

    public void schedule(WorkRequest workRequest, long id) {

        ScheduledWorkRequest scheduledWorkRequest = new ScheduledWorkRequest(workRequest);

        // If we have a worker thread pool, use that; otherwise, do the work
        // directly.
        int size = workers.size();
        if (size > 0) {
            try {
                int workerNum = ((int) (id % size) + size) % size;
                ExecutorService worker = workers.get(workerNum);
                worker.execute(scheduledWorkRequest);
            } catch (Exception e) {
                System.out.println("ExecutorService rejected execution" + e);
                workRequest.cleanup();
            }
        } else {
            scheduledWorkRequest.run();
        }
    }

    private class ScheduledWorkRequest implements Runnable {

        private final WorkRequest workRequest;

        ScheduledWorkRequest(WorkRequest workRequest) {
            this.workRequest = workRequest;
        }

        @Override
        public void run() {
            try {
                workRequest.doWork();
            } catch (Exception e) {
                System.out.println("Unexpected exception" + e);
                workRequest.cleanup();
            }
        }
    }

    /**
     * 调用者需要实现 WorkRequest，以便通过线程池来调度任务
     */
    public abstract static class WorkRequest {

        /**
         * Must be implemented. Is called when the work request is run.
         */
        public abstract void doWork() throws Exception;

        /**
         * (Optional) If implemented, is called if the service is stopped
         * or unable to schedule the request.
         */
        public void cleanup() {
        }

    }

    private static class DaemonThreadFactory implements ThreadFactory {

        final ThreadGroup group;
        final AtomicInteger threadNumber = new AtomicInteger(1);
        final String namePrefix;

        DaemonThreadFactory(String name) {
            this(name, 1);
        }

        DaemonThreadFactory(String name, int firstThreadNum) {
            threadNumber.set(firstThreadNum);
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = name + "-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (!t.isDaemon()) {
                t.setDaemon(true);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }

    }
}
