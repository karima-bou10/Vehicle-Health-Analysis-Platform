package com.renault.telemetry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "analysisExecutor")
    public Executor analysisExecutor() {
        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2); // Deux threads disponibles
        executor.setMaxPoolSize(4); // Jusqu’à quatre traitements simultanés
        executor.setQueueCapacity(50); // Jusqu’à 50 tâches peuvent attendre
        executor.setThreadNamePrefix("vehicle-analysis-");
        executor.initialize();

        return executor;
    }
}
