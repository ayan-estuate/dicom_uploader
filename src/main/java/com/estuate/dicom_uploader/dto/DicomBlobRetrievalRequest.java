package com.estuate.dicom_uploader.dto;

import lombok.Data;

@Data
public class DicomBlobRetrievalRequest {
    private String projectId;
    private String bucket;
    private String blobPath;
    private String objectKey; // Target S3 key
}