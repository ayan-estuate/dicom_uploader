package com.estuate.dicom_uploader.async;

import com.estuate.dicom_uploader.model.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobScheduler {

    private final JobQueueManager jobQueueManager;
    private final JobProcessor jobProcessor;

    // Thread pool with fixed number of workers, can be configurable
    private final ExecutorService executor = Executors.newFixedThreadPool(5);

    // Runs every 10 seconds to pick and process queued jobs
    @Scheduled(fixedDelay = 10000)
    public void scheduleJobs() {
        try {
            List<Job> queuedJobs = jobQueueManager.getQueuedJobs();
            for (Job job : queuedJobs) {
                log.info("Scheduling job {}", job.getJobId());
                executor.submit(() -> jobProcessor.processJob(job));
            }
        } catch (IOException e) {
            log.error("Failed to fetch queued jobs: {}", e.getMessage(), e);
        }
    }
}
