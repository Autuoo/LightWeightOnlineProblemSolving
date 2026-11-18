package com.example.oj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ExecutorConfig {

    @Bean(name = "judgeExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor judgeExecutor(JudgeProperties properties) {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "judge-worker-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return new ThreadPoolExecutor(
                properties.getConcurrency(),
                properties.getConcurrency(),
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(properties.getQueueCapacity()),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }
}