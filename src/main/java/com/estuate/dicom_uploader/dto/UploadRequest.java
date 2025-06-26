package com.estuate.dicom_uploader.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UploadRequest {

    @NotBlank
    private final String objectKey;

    @NotBlank
    private final String platform;

//    @NotBlank
    private final String storageType;

    // Native (Healthcare API)
    private final String datasetName;
    private final String dicomStoreName;

    // Blob storage
    private final String bucketName;      // for GCP
    private final String blobContainer;   // for Azure
    private final String blobPath;        // common blob path

    public boolean isNative() {
        return "native".equalsIgnoreCase(storageType);
    }

    public boolean isBlob() {
        return "blob".equalsIgnoreCase(storageType);
    }
}
