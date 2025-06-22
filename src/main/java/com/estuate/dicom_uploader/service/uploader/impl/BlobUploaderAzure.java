package com.estuate.dicom_uploader.service.uploader.impl;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.azure.AzureUploaderService;
import com.estuate.dicom_uploader.service.uploader.CloudUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlobUploaderAzure implements CloudUploader {
    private final AzureUploaderService azureUploaderService;

    @Override
    public void upload(byte[] fileData, Job job) throws Exception {
        azureUploaderService.uploadToAzureBlob(fileData, job);
    }

    @Override
    public boolean supports(String platform, String storageType) {
        return "azure".equalsIgnoreCase(platform) && "blob".equalsIgnoreCase(storageType);
    }
}
