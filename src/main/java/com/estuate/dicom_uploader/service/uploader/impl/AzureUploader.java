package com.estuate.dicom_uploader.service.uploader.impl;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.azure.AzureUploaderService;
import com.estuate.dicom_uploader.service.uploader.CloudUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AzureUploader implements CloudUploader {
    private final AzureUploaderService azureUploaderService;

    @Override
    public void upload(byte[] dicomData, Job job) throws Exception {
        azureUploaderService.uploadToAzure(dicomData, job);
    }

    @Override
    public boolean supports(String platform, String storageType) {
        return "azure".equalsIgnoreCase(platform) && "native".equalsIgnoreCase(storageType);
    }
}
