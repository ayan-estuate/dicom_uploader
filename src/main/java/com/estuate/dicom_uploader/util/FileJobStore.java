package com.estuate.dicom_uploader.util;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.model.JobStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@Component // This makes the class a Spring-managed component (like a helper)
@Slf4j     // This allows you to use the 'log' object for logging errors
public class FileJobStore {

    // This is the base folder where all job files will be saved
    private static final String JOBS_BASE_DIR = "jobs";

    private final ObjectMapper objectMapper;

    // Spring will automatically give us an ObjectMapper (used for reading/writing JSON)
    @Autowired
    public FileJobStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * This method saves a job object to a file.
     * The file is saved as JSON in a folder based on the job's status.
     */
    public void saveJob(Job job) throws IOException {
        // Get the directory where we want to save this job (based on status)
        Path dir = getDirPath(job.getStatus());

        // If the folder doesn't exist, create it
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // Build the file name like jobs/pending/job123.json
        Path jobFile = dir.resolve(job.getJobId() + ".json");

        // Save the job object to the file as JSON
        objectMapper.writeValue(jobFile.toFile(), job);
    }

    /**
     * This method moves a job file from one folder to another
     * (for example: from pending to completed).
     */
    public void moveJob(Job job, JobStatus newStatus) throws IOException {
        // Get the old file path (where the job currently is)
        Path oldFile = getDirPath(job.getStatus()).resolve(job.getJobId() + ".json");

        // Get the new folder path (where the job should go)
        Path newDir = getDirPath(newStatus);

        // If the new folder doesn't exist, create it
        if (!Files.exists(newDir)) {
            Files.createDirectories(newDir);
        }

        // Build the new file path inside the new folder
        Path newFile = newDir.resolve(job.getJobId() + ".json");

        // Move the file to the new location (replace if already exists)
        Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);

        // Update the job's status and time
        job.setStatus(newStatus);
        job.setUpdatedAt(java.time.Instant.now());

        // Save the updated job
        saveJob(job);
    }

    /**
     * This method loads (reads) a job file by its ID and status.
     * It returns the Job object from the file.
     */
    public Job loadJob(JobStatus status, String jobId) throws IOException {
        // Build the path to the job file (like jobs/pending/job123.json)
        Path jobFile = getDirPath(status).resolve(jobId + ".json");

        // If the file doesn't exist, return null
        if (!Files.exists(jobFile)) {
            return null;
        }

        // Read and return the job object from the JSON file
        return objectMapper.readValue(jobFile.toFile(), Job.class);
    }

    /**
     * This method lists all the job files under a specific status folder.
     * It returns a list of Job objects.
     */
    public List<Job> listJobs(JobStatus status) throws IOException {
        // Get the folder path for the given status
        Path dir = getDirPath(status);

        // List to hold all jobs we find
        List<Job> jobs = new ArrayList<>();

        // If the folder doesn't exist, return an empty list
        if (!Files.exists(dir)) {
            return jobs;
        }

        // Look for all .json files in the folder
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path path : stream) {
                try {
                    // Read each JSON file and convert it to a Job object
                    Job job = objectMapper.readValue(path.toFile(), Job.class);
                    jobs.add(job);
                } catch (Exception e) {
                    // If there's a problem with one file, skip it but show an error
                    log.error("Failed to read job file {}", path, e);
                }
            }
        }

        return jobs; // Return all the jobs we were able to read
    }

    /**
     * This helper method returns the folder path for a given job status.
     * For example: jobs/pending or jobs/completed
     */
    private Path getDirPath(JobStatus status) {
        return Paths.get(JOBS_BASE_DIR, status.name().toLowerCase());
    }
}
