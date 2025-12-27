package com.fam.vest.algo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    @Bean(name = "straddleExecutor")
    public ThreadPoolTaskExecutor straddleExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);      // Minimum threads
        executor.setMaxPoolSize(30);       // Max concurrent threads
        executor.setQueueCapacity(20);     // Tasks waiting in queue
        executor.setThreadNamePrefix("StraddleExecutor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    @Bean(name = "straddleScheduler")
    public ScheduledExecutorService straddleScheduler(
            @Value("${straddle.scheduler.pool-size:20}") int poolSize) {
        return Executors.newScheduledThreadPool(poolSize, new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "StraddleScheduler-" + counter.getAndIncrement());
            }
        });
    }
}
