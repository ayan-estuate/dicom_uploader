package com.estuate.dicom_uploader.async;

import com.estuate.dicom_uploader.exception.DicomUploadException;
import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.model.JobStatus;
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
    private final S3PresignedUrlService generatePresignedUrl;

    public void processJob(Job job) {
        try {
            jobQueueManager.updateJobStatus(job, JobStatus.IN_PROGRESS);



            String presignedUrl = generatePresignedUrl.generatePresignedUrl(job.getObjectKey()).toString();

            dicomUploaderService.upload(presignedUrl, job.getPlatform());

            jobQueueManager.updateJobStatus(job, JobStatus.COMPLETED);
            log.info("Job {} completed successfully", job.getJobId());

        } catch (IOException | ParseException | DicomUploadException e) {
            log.error("Job {} failed: {}", job.getJobId(), e.getMessage(), e);
            try {
                job.setErrorMessage(e.getMessage());
                jobQueueManager.updateJobStatus(job, JobStatus.FAILED);
            } catch (IOException ioException) {
                log.error("Failed to update job status to FAILED for job {}: {}", job.getJobId(), ioException.getMessage());
            }
        }
    }
}
