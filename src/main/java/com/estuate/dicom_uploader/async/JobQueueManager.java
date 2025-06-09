package com.estuate.dicom_uploader.async;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.model.JobStatus;
import com.estuate.dicom_uploader.util.FileJobStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JobQueueManager {

    private final FileJobStore jobStore;

    public Job enqueueJob(String objectKey, String platform) throws IOException {
        Job job = Job.builder()
                .jobId(UUID.randomUUID().toString())
                .objectKey(objectKey)
                .platform(platform)
                .status(JobStatus.QUEUED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        jobStore.saveJob(job);
        return job;
    }

    public List<Job> getQueuedJobs() throws IOException {
        return jobStore.listJobs(JobStatus.QUEUED);
    }

    public void updateJobStatus(Job job, JobStatus newStatus) throws IOException {
        jobStore.moveJob(job, newStatus);
    }

    public Job getJob(JobStatus status, String jobId) throws IOException {
        return jobStore.loadJob(status, jobId);
    }
}
