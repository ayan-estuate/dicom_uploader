package com.estuate.dicom_uploader.controller;

import com.estuate.dicom_uploader.async.JobQueueManager;
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
@RequiredArgsConstructor
@Validated
@Slf4j
public class DicomController {

    private final JobQueueManager jobQueueManager;

    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("gcp", "azure");

    public record UploadRequest(@NotBlank String objectKey, @NotBlank String platform) {}
    public record UploadResponse(String status, String message, String jobId) {}

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDicom(@RequestBody @Valid UploadRequest request) throws IOException {
        String platformLower = request.platform().toLowerCase();

        if (!SUPPORTED_PLATFORMS.contains(platformLower)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("error", "Unsupported platform: " + request.platform(), null));
        }

        Job job = jobQueueManager.enqueueJob(request.objectKey(), platformLower);

        return ResponseEntity.ok(new UploadResponse("success", "Job queued", job.getJobId()));
    }

    // Optional: Add API to query job status by jobId
    @GetMapping("/status")
    public ResponseEntity<?> getJobStatus(@RequestParam String jobId) throws IOException {
        for (JobStatus status : JobStatus.values()) {
            Job job = jobQueueManager.getJob(status, jobId);
            if (job != null) {
                return ResponseEntity.ok(job);
            }
        }
        return ResponseEntity.notFound().build();
    }

}
