package com.estuate.dicom_uploader.dto;

import lombok.Data;

@Data
public class DicomRetrievalRequest {
    private String platform;        // e.g., "gcp", "azure"
    private String storageType;     // e.g., "native", "blob"
    private String objectKey;       // destination key for S3 (e.g., "roundtrip/sample.dcm")

    // Native DICOM (Healthcare API) retrieval fields
    private String projectId;
    private String location;
    private String dataset;
    private String dicomStore;
    private String studyUid;
    private String seriesUid;
    private String instanceUid;

    // Blob-based (GCS) retrieval fields
    private String bucket;          // GCP bucket name (for blob source)
    private String blobPath;        // Full path to blob in GCP bucket
}
