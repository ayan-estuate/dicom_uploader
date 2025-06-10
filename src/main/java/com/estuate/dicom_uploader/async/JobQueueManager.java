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

@Component // This tells Spring to create and manage this class as a bean
@RequiredArgsConstructor // Automatically creates a constructor for all final fields
public class JobQueueManager {

    // This helper class handles reading and writing job files
    private final FileJobStore jobStore;

    /**
     * This method creates a new Job and saves it to the QUEUED folder.
     *
     * @param objectKey A unique file name or object identifier (like a DICOM file name)
     * @param platform  Platform name (e.g., "GCP", "AWS", etc.)
     * @return The Job that was created and saved
     * @throws IOException If saving the job fails
     */
    public Job enqueueJob(String objectKey, String platform) throws IOException {
        // Create a new Job object with unique ID and QUEUED status
        Job job = Job.builder()
                .jobId(UUID.randomUUID().toString()) // Generate random job ID
                .objectKey(objectKey)
                .platform(platform)
                .status(JobStatus.QUEUED) // Set initial status
                .createdAt(Instant.now()) // Set creation time
                .updatedAt(Instant.now()) // Set last updated time
                .build();

        // Save the job to a JSON file in the queued folder
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
