package com.jnet.anno.test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/8/8 09:25:11
 */

public class ThreadPoolUtil {

    private static final int CORE_POOL_SIZE = 50;
    private static final int MAXIMUM_POOL_SIZE = 100;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final BlockingQueue<Runnable> WORK_QUEUE = new LinkedBlockingQueue<>(100);

    private static ExecutorService executorService;

    private ThreadPoolUtil() {
        throw new AssertionError("Cannot instantiate ThreadPoolUtil");
    }

    public static ExecutorService getExecutorService() {
        if (executorService == null) {
            synchronized (ThreadPoolUtil.class) {
                if (executorService == null) {
                    executorService = new ThreadPoolExecutor(
                            CORE_POOL_SIZE,
                            MAXIMUM_POOL_SIZE,
                            KEEP_ALIVE_TIME,
                            TIME_UNIT,
                            WORK_QUEUE,
                            new NamedThreadFactory("ThreadPool-"),
                            new ThreadPoolExecutor.CallerRunsPolicy()
                    );
                }
            }
        }
        return executorService;
    }

    public static void execute(Runnable task) {
        ExecutorService service = getExecutorService();
        if (service != null) {
            service.execute(task);
        } else {
            throw new IllegalStateException("Executor service is not initialized.");
        }
    }

    public static void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    public static void shutdownNow() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    // 内部类用于命名线程
    private static class NamedThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        NamedThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            return t;
        }
    }
}

