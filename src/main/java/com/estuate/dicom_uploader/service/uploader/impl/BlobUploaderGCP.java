package com.estuate.dicom_uploader.service.uploader.impl;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.gcp.GCPUploaderService;
import com.estuate.dicom_uploader.service.uploader.CloudUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlobUploaderGCP implements CloudUploader {
    private final GCPUploaderService gcpUploaderService;

    @Override
    public void upload(byte[] fileData, Job job) throws Exception {
        gcpUploaderService.uploadToGCPBlob(fileData, job);
    }

    @Override
    public boolean supports(String platform, String storageType) {
        return "gcp".equalsIgnoreCase(platform) && "blob".equalsIgnoreCase(storageType);
    }
}
