package com.chatapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Thread pool configuration leveraging Java 21 Virtual Threads.
 *
 * Virtual threads allow massive concurrency with minimal overhead.
 * Each WebSocket connection and message handler runs on its own virtual thread.
 */
@Configuration
public class ThreadingConfig implements AsyncConfigurer {

    /**
     * Primary async executor using Java 21 virtual threads.
     * Supports millions of concurrent lightweight threads.
     */
    @Bean(name = "virtualThreadExecutor")
    public Executor virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Bounded thread pool for CPU-intensive tasks.
     */
    @Bean(name = "cpuBoundExecutor")
    public ThreadPoolTaskExecutor cpuBoundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("cpu-worker-");
        executor.initialize();
        return executor;
    }

    /**
     * Default async executor — virtual threads.
     */
    @Override
    public Executor getAsyncExecutor() {
        return virtualThreadExecutor();
    }
}
