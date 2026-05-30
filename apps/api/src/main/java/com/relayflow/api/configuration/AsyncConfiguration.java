package com.relayflow.api.configuration;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@Configuration
public class AsyncConfiguration {

    /**
     * Dedicated thread pool for webhook delivery.
     *
     * <p>Core=4, max=16, queue=500 keeps webhook delivery isolated from the main request-handling
     * threads and provides basic back-pressure.
     */
    @Bean(name = "webhookExecutor")
    Executor webhookExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("webhook-");
        executor.initialize();

        return executor;
    }
}
