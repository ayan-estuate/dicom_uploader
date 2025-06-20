package com.estuate.dicom_uploader.async;

import com.estuate.dicom_uploader.dto.DicomRetrievalRequest;
import com.estuate.dicom_uploader.dto.UploadRequest;
import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.model.JobStatus;
import com.estuate.dicom_uploader.model.JobType;
import com.estuate.dicom_uploader.util.FileJobStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component // This tells Spring to create and manage this class as a bean
@RequiredArgsConstructor // Automatically creates a constructor for all final fields
public class JobQueueManager {

    // This helper class handles reading and writing job files
    private final FileJobStore jobStore;

    public Job enqueueUploadJob(UploadRequest request) throws IOException {
        Job job = Job.builder()
                .jobId(UUID.randomUUID().toString())
                .jobType(JobType.UPLOAD)
                .objectKey(request.getObjectKey())
                .platform(request.getPlatform())
                .storageType(request.getStorageType())
                .datasetName(request.getDatasetName())
                .dicomStoreName(request.getDicomStoreName())
                .bucketName(request.getBucketName())
                .blobContainer(request.getBlobContainer())
                .blobPath(request.getBlobPath())
                .status(JobStatus.QUEUED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        jobStore.saveJob(job);
        return job;
    }

    public Job enqueueRetrievalJob(DicomRetrievalRequest request) throws IOException {
        Job job = Job.builder()
                .jobId(UUID.randomUUID().toString())
                .jobType(JobType.RETRIEVAL)
                .objectKey(request.getObjectKey())
                .platform(request.getPlatform())
                .storageType(request.getStorageType())
                .datasetName(request.getDataset())
                .dicomStoreName(request.getDicomStore())
                .gcpProjectId(request.getProjectId())
                .location(request.getLocation())
                .studyUid(request.getStudyUid())
                .seriesUid(request.getSeriesUid())
                .instanceUid(request.getInstanceUid())
                .bucketName(request.getBucket())
                .blobPath(request.getBlobPath())
                .status(JobStatus.QUEUED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        jobStore.saveJob(job);
        return job;
    }
    /**
     * This method returns all the jobs that are currently in the QUEUED state.
     *
     * @return List of queued jobs
     * @throws IOException If reading job files fails
     */
    public List<Job> getQueuedJobs() throws IOException {
        // Return all jobs found in jobs/queued folder
        return jobStore.listJobs(JobStatus.QUEUED);
    }

    /**
     * This method changes the status of an existing job.
     * For example, QUEUED → PROCESSING → COMPLETED.
     *
     * @param job       The job to be updated
     * @param newStatus The new status to set
     * @throws IOException If file move or update fails
     */
    public void updateJobStatus(Job job, JobStatus newStatus) throws IOException {
        // Move the job file to the new status folder
        jobStore.moveJob(job, newStatus);
    }

    /**
     * This method loads (fetches) a specific job by its ID and current status.
     *
     * @param status The current status folder (e.g., QUEUED, COMPLETED)
     * @param jobId  The unique ID of the job
     * @return The Job object, or null if not found
     * @throws IOException If reading the file fails
     */
    public Job getJob(JobStatus status, String jobId) throws IOException {
        // Read and return the job from its current folder
        return jobStore.loadJob(status, jobId);
    }
}
