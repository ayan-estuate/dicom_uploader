package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.azure.AzureRetrievalService;
import com.estuate.dicom_uploader.service.gcp.GCPRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DicomRetrievalService {

    private final GCPRetrievalService gcpRetrievalService;
    private final AzureRetrievalService azureRetrievalService;

    @Value("${azure.blob.connection-string}")
    private String connectionString;

    public void retrieveAndUpload(Job job) throws IOException {
        String platform = job.getPlatform();
        String storageType = job.getStorageType();

        switch (platform.toLowerCase()) {
            case "gcp" -> {
                if ("native".equalsIgnoreCase(storageType)) {
                    gcpRetrievalService.retrieveNativeAndUploadToS3(
                            job.getGcpProjectId(), job.getLocation(), job.getDatasetName(),
                            job.getDicomStoreName(), job.getStudyUid(), job.getSeriesUid(), job.getInstanceUid(),
                            job.getObjectKey());
                } else {
                    gcpRetrievalService.retrieveFromGCPBlobAndUploadToS3(
                            job.getGcpProjectId(), job.getBucketName(), job.getObjectKey(), job.getObjectKey());
                }
            }

            case "azure" -> {
                if ("native".equalsIgnoreCase(storageType)) {
                    azureRetrievalService.retrieveNativeAndUploadToS3(
                            job.getStudyUid(), job.getSeriesUid(), job.getInstanceUid());
                } else if ("blob".equalsIgnoreCase(storageType)) {
                    azureRetrievalService.retrieveFromGCPBlobAndUploadToS3(
                            connectionString, job.getBlobContainer(), job.getObjectKey(), job.getObjectKey(), job);
                } else {
                    throw new IllegalArgumentException("Unsupported storage type for Azure: " + storageType);
                }
            }

            default -> throw new IllegalArgumentException("Unsupported platform: " + platform);
        }
    }
}

