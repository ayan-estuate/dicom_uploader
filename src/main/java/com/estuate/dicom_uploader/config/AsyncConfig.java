package com.estuate.dicom_uploader.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("uploadExecutor")
    public ThreadPoolTaskExecutor asyncUploadExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);  // can tune this
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("upload-async-");
        executor.initialize();
        return executor;
    }
    @Bean("bucketExecutor")
    public ExecutorService bucketExecutor() {
        return Executors.newCachedThreadPool();
    }
}
