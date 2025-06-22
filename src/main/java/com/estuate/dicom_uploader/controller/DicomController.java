package com.estuate.dicom_uploader.controller;

import com.estuate.dicom_uploader.async.JobQueueManager;
import com.estuate.dicom_uploader.dto.DicomRetrievalRequest;
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
@RequiredArgsConstructor
@Validated
@Slf4j
public class DicomController {

    private final JobQueueManager jobQueueManager;

    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("gcp", "azure");
    private static final Set<String> SUPPORTED_STORAGE_TYPES = Set.of("native", "blob");

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDicom(@RequestBody @Valid UploadRequest request) throws IOException {
        String platform = request.getPlatform().toLowerCase();
        String storageType = request.getStorageType().toLowerCase();

        if (!SUPPORTED_PLATFORMS.contains(platform)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("error", "Unsupported platform: " + request.getPlatform(), null));
        }

        if (!SUPPORTED_STORAGE_TYPES.contains(storageType)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("error", "Unsupported storage type: " + request.getStorageType(), null));
        }

        // Use switch expression with pattern matching-style branching
        switch (storageType) {
            case "native" -> {
//                if (request.getDatasetName() == null || request.getDicomStoreName() == null) {
//                    return ResponseEntity.badRequest()
//                            .body(new UploadResponse("error", "Missing datasetName or dicomStoreName for native storage", null));
//                }
                log.info("Validated native storage for platform: {}", platform);
            }
            case "blob" -> {
                switch (platform) {
                    case "gcp" -> {
                        if (request.getBucketName() == null || request.getBlobPath() == null) {
                            return ResponseEntity.badRequest()
                                    .body(new UploadResponse("error", "Missing bucketName or blobPath for GCP blob storage", null));
                        }
                    }
                    case "azure" -> {
                        if (request.getBlobContainer() == null || request.getBlobPath() == null) {
                            return ResponseEntity.badRequest()
                                    .body(new UploadResponse("error", "Missing blobContainer or blobPath for Azure blob storage", null));
                        }
                    }
                    default -> {
                        return ResponseEntity.badRequest()
                                .body(new UploadResponse("error", "Unsupported platform for blob storage: " + platform, null));
                    }
                }
                log.info("Validated blob storage for platform: {}", platform);
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(new UploadResponse("error", "Unhandled storage type: " + storageType, null));
            }
        }

        Job job = jobQueueManager.enqueueUploadJob(request);
        return ResponseEntity.ok(new UploadResponse("success", "Job queued", job.getJobId()));
    }

    @PostMapping("/retrieve")
    public ResponseEntity<UploadResponse> retrieveDicom(@RequestBody @Valid DicomRetrievalRequest request) throws IOException {
        String platform = request.getPlatform().toLowerCase();
        String storageType = request.getStorageType().toLowerCase();

        if (!SUPPORTED_PLATFORMS.contains(platform)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("error", "Unsupported platform: " + request.getPlatform(), null));
        }

        if (!SUPPORTED_STORAGE_TYPES.contains(storageType)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("error", "Unsupported storage type: " + request.getStorageType(), null));
        }

        switch (storageType) {
            case "native" -> {
                if (request.getDataset() == null || request.getDicomStore() == null ||
                        request.getStudyUid() == null || request.getSeriesUid() == null || request.getInstanceUid() == null) {
                    return ResponseEntity.badRequest()
                            .body(new UploadResponse("error", "Missing required fields for native retrieval", null));
                }
                log.info("Validated native retrieval for platform: {}", platform);
            }
            case "blob" -> {
                if (request.getBucket() == null || request.getBlobPath() == null) {
                    return ResponseEntity.badRequest()
                            .body(new UploadResponse("error", "Missing bucketName or blobPath for blob retrieval", null));
                }
                log.info("Validated blob retrieval for platform: {}", platform);
            }
            default -> {
                return ResponseEntity.badRequest()
                        .body(new UploadResponse("error", "Unhandled storage type: " + storageType, null));
            }
        }

        Job job = jobQueueManager.enqueueRetrievalJob(request);
        return ResponseEntity.ok(new UploadResponse("success", "Retrieval job queued", job.getJobId()));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getJobStatus(@RequestParam @NotBlank String jobId) throws IOException {
        for (JobStatus status : JobStatus.values()) {
            Job job = jobQueueManager.getJob(status, jobId);
            if (job != null) {
                return ResponseEntity.ok(job);
            }
        }
        return ResponseEntity.notFound().build();
    }
}
