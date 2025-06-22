package com.estuate.dicom_uploader.service;


import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.gcp.GCPRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DicomRetrievalService {

    private final GCPRetrievalService gcpRetrievalService;
//    private final AzureRetrievalService azureRetrievalService; // ← to be implemented

    public void retrieveAndUpload(Job job) {
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
                            job.getGcpProjectId(), job.getBucketName(), job.getBlobPath(), job.getObjectKey());
                }
            }
            case "azure" -> {
                // Call AzureRetrievalService when implemented
//                azureRetrievalService.retrieveAndUpload(job);
            }
            default -> throw new IllegalArgumentException("Unsupported platform: " + platform);
        }
    }
}

