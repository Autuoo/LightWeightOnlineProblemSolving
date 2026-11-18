package com.example.oj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AiExecutorConfig {

    @Bean(name = "aiExecutor", destroyMethod = "shutdown")
    public ExecutorService aiExecutor(AiProperties properties) {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable, "ai-worker-" + seq.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(Math.max(1, properties.getConcurrency()), threadFactory);
    }
}