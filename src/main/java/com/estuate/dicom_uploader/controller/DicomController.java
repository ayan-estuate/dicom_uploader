package com.estuate.dicom_uploader.controller;

import com.estuate.dicom_uploader.async.JobQueueManager;
import com.estuate.dicom_uploader.dto.UploadRequest;
import com.estuate.dicom_uploader.dto.UploadResponse;
import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.model.JobStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Set;

@RestController
@RequestMapping("/dicom")
@RequiredArgsConstructor // This creates a constructor with required (final) fields
@Validated // Enables validation on incoming request data
@Slf4j // Enables logging (you can use log.info(), log.error(), etc.)
public class DicomController {

    // This handles job creation and job status logic
    private final JobQueueManager jobQueueManager;

    // List of platforms we currently support
    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("gcp", "azure");

    // This endpoint handles POST requests to /dicom/upload
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDicom(@RequestBody @Valid UploadRequest request) throws IOException {
        // Convert platform name to lowercase (so GCP and gcp are treated the same)
        String platformLower = request.platform().toLowerCase();

        // If the platform is not supported (like aws or some other), return error
        if (!SUPPORTED_PLATFORMS.contains(platformLower)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("error", "Unsupported platform: " + request.platform(), null));
        }

        // Create and save a new job using the JobQueueManager
        Job job = jobQueueManager.enqueueJob(request.objectKey(), platformLower);

        // Return a success response with job ID
        return ResponseEntity.ok(new UploadResponse("success", "Job queued", job.getJobId()));
    }

    // This endpoint handles GET requests to /dicom/status?jobId=xyz
    // It checks the job status for the given jobId
    @GetMapping("/status")
    public ResponseEntity<?> getJobStatus(@RequestParam String jobId) throws IOException {
        // Loop through all possible job statuses (QUEUED, PROCESSING, etc.)
        for (JobStatus status : JobStatus.values()) {
            // Try to find the job with that status and jobId
            Job job = jobQueueManager.getJob(status, jobId);
            if (job != null) {
                // If found, return the job details
                return ResponseEntity.ok(job);
            }
        }
        // If not found in any status, return 404 (not found)
        return ResponseEntity.notFound().build();
    }
}
