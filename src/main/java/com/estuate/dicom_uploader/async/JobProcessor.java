package com.estuate.dicom_uploader.async;

import com.estuate.dicom_uploader.exception.DicomUploadException;
import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.model.JobStatus;
import com.estuate.dicom_uploader.model.JobType;
import com.estuate.dicom_uploader.service.DicomRetrievalService;
import com.estuate.dicom_uploader.service.DicomUploaderService;
import com.estuate.dicom_uploader.service.S3PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobProcessor {

    private final JobQueueManager jobQueueManager;
    private final DicomUploaderService dicomUploaderService;
    private final S3PresignedUrlService presignedUrlService;
    private final DicomRetrievalService dicomRetrievalService;

    public void processJob(Job job) {
        try {
            jobQueueManager.updateJobStatus(job, JobStatus.IN_PROGRESS);

            if (job.getJobType() == JobType.UPLOAD) {
                processUploadJob(job);
            } else if (job.getJobType() == JobType.RETRIEVAL) {
                processRetrievalJob(job);
            } else {
                throw new IllegalStateException("Unknown job type: " + job.getJobType());
            }

            jobQueueManager.updateJobStatus(job, JobStatus.COMPLETED);
            log.info("Job {} completed successfully", job.getJobId());

        } catch (Exception e) {
            log.error("Job {} failed: {}", job.getJobId(), e.getMessage(), e);
            try {
                job.setErrorMessage(e.getMessage());
                jobQueueManager.updateJobStatus(job, JobStatus.FAILED);
            } catch (IOException ioException) {
                log.error("Failed to update status to FAILED for job {}: {}", job.getJobId(), ioException.getMessage());
            }
        }
    }

    private void processUploadJob(Job job) throws IOException, ParseException, DicomUploadException {
        String presignedUrl = presignedUrlService.generatePresignedUrl(job.getObjectKey()).toString();
        dicomUploaderService.upload(presignedUrl, job);
        log.info("Upload job {} processed successfully", job.getJobId());
    }

    private void processRetrievalJob(Job job) throws IOException {
        dicomRetrievalService.retrieveAndUpload(job);
        log.info("Retrieval job {} processed successfully", job.getJobId());
    }
}
