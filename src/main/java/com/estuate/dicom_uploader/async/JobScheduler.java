package com.estuate.dicom_uploader.async;

import com.estuate.dicom_uploader.model.Job;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


@Slf4j
@Component
public class JobScheduler {

    private final JobQueueManager jobQueueManager;
    private final JobProcessor jobProcessor;

    @Qualifier("uploadExecutor") // ← from your AsyncConfig
    private final ThreadPoolTaskExecutor uploadExecutor;

    @Autowired
    public JobScheduler(
            JobQueueManager jobQueueManager,
            JobProcessor jobProcessor,
            @Qualifier("uploadExecutor") ThreadPoolTaskExecutor uploadExecutor
    ) {
        this.jobQueueManager = jobQueueManager;
        this.jobProcessor = jobProcessor;
        this.uploadExecutor = uploadExecutor;
    }

    @Scheduled(fixedDelay = 10000)
    public void scheduleJobs() {
        try {
            List<Job> queuedJobs = jobQueueManager.getQueuedJobs();
            for (Job job : queuedJobs) {
                log.info("Submitting job {} to thread pool", job.getJobId());
                uploadExecutor.submit(() -> jobProcessor.processJob(job));
            }
        } catch (IOException e) {
            log.error("Failed to fetch queued jobs: {}", e.getMessage(), e);
        }
    }
}
