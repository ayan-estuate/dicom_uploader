package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.uploader.CloudUploader;
import com.estuate.dicom_uploader.util.DicomValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;

import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.retry.annotation.Backoff;
import javax.net.ssl.SSLException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Future;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadOrchestratorService {

    private final DownloaderService downloaderService;
    private final CloudUploaderFactory uploaderFactory;

    @Async("uploadExecutor")
    @Retryable(
            value = { IOException.class, SSLException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public Future<Void> orchestrateUploadAsync(String presignedUrl, Job job) throws Exception {
        File tempDicom = downloaderService.downloadToTempFile(presignedUrl, job.getJobId());
        byte[] dicomBytes = null;

        try {
            dicomBytes = Files.readAllBytes(tempDicom.toPath()); // Read once

            // Validate if native
            if ("native".equalsIgnoreCase(job.getStorageType()) &&
                    !DicomValidator.isValidDicom(dicomBytes)) {
                throw new IllegalArgumentException("Invalid DICOM file.");
            }

            CloudUploader uploader = uploaderFactory.resolve(job);
            uploader.upload(dicomBytes, job); // Use same bytes
            return AsyncResult.forValue(null);

        } finally {
            if (tempDicom.exists() && !tempDicom.delete()) {
                log.warn("Failed to delete temp file {}", tempDicom.getAbsolutePath());
            }
        }
    }
}
