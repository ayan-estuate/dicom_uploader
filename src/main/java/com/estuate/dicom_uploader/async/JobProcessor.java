package com.estuate.dicom_uploader.async;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.model.JobStatus;
import com.estuate.dicom_uploader.model.JobType;
import com.estuate.dicom_uploader.service.S3PresignedUrlService;
import com.estuate.dicom_uploader.service.UploadOrchestratorService;
import com.estuate.dicom_uploader.service.DicomRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProcessor {

    private final JobQueueManager jobQueueManager;
    private final S3PresignedUrlService presignedUrlService;
    private final UploadOrchestratorService uploadOrchestratorService;
    private final DicomRetrievalService dicomRetrievalService;

    private static final ConcurrentHashMap<String, Object> activeObjectKeys = new ConcurrentHashMap<>();

    public void processJob(Job job) {
        Object lock = activeObjectKeys.computeIfAbsent(job.getObjectKey(), k -> new Object());

        synchronized (lock) {
            try {
                jobQueueManager.updateJobStatus(job, JobStatus.IN_PROGRESS);

                if (job.getJobType() == JobType.UPLOAD) {
                    handleAsyncUpload(job);
                } else if (job.getJobType() == JobType.RETRIEVAL) {
                    dicomRetrievalService.retrieveAndUpload(job);
                } else {
                    throw new IllegalStateException("Unsupported job type: " + job.getJobType());
                }

                jobQueueManager.updateJobStatus(job, JobStatus.COMPLETED);
                log.info("✅ Job {} completed successfully", job.getJobId());

            } catch (Exception e) {
                log.error("❌ Job {} failed: {}", job.getJobId(), e.getMessage(), e);
                try {
                    job.setErrorMessage(e.getMessage());
                    jobQueueManager.updateJobStatus(job, JobStatus.FAILED);
                } catch (IOException ioEx) {
                    log.error("Failed to mark job {} as FAILED", job.getJobId(), ioEx);
                }
            } finally {
                activeObjectKeys.remove(job.getObjectKey());
            }
        }
    }

    private void handleAsyncUpload(Job job) throws Exception {
        String presignedUrl = presignedUrlService.generatePresignedUrl(job.getObjectKey()).toString();

        // Async but still handled inside current job thread
        uploadOrchestratorService.orchestrateUploadAsync(presignedUrl, job).get(); // .get() ensures blocking for job status tracking
    }
}
