package com.tenantguard.serviceb.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

    @Bean
    public ThreadPoolTaskExecutor consumerExecutor(@Value("${tenantguard.consumer.threads:1}") int threads) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(threads);
        executor.setMaxPoolSize(threads);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("consumer-");
        executor.initialize();
        return executor;
    }
}