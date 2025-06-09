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

@Component
@Slf4j
public class FileJobStore {

    private static final String JOBS_BASE_DIR = "jobs";

    private final ObjectMapper objectMapper;

    @Autowired
    public FileJobStore(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void saveJob(Job job) throws IOException {
        Path dir = getDirPath(job.getStatus());
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path jobFile = dir.resolve(job.getJobId() + ".json");
        objectMapper.writeValue(jobFile.toFile(), job);
    }

    public void moveJob(Job job, JobStatus newStatus) throws IOException {
        Path oldFile = getDirPath(job.getStatus()).resolve(job.getJobId() + ".json");
        Path newDir = getDirPath(newStatus);
        if (!Files.exists(newDir)) {
            Files.createDirectories(newDir);
        }
        Path newFile = newDir.resolve(job.getJobId() + ".json");
        Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
        job.setStatus(newStatus);
        job.setUpdatedAt(java.time.Instant.now());
        saveJob(job);
    }

    public Job loadJob(JobStatus status, String jobId) throws IOException {
        Path jobFile = getDirPath(status).resolve(jobId + ".json");
        if (!Files.exists(jobFile)) {
            return null;
        }
        return objectMapper.readValue(jobFile.toFile(), Job.class);
    }

    public List<Job> listJobs(JobStatus status) throws IOException {
        Path dir = getDirPath(status);
        List<Job> jobs = new ArrayList<>();
        if (!Files.exists(dir)) {
            return jobs;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.json")) {
            for (Path path : stream) {
                try {
                    Job job = objectMapper.readValue(path.toFile(), Job.class);
                    jobs.add(job);
                } catch (Exception e) {
                    log.error("Failed to read job file {}", path, e);
                }
            }
        }
        return jobs;
    }

    private Path getDirPath(JobStatus status) {
        return Paths.get(JOBS_BASE_DIR, status.name().toLowerCase());
    }
}
