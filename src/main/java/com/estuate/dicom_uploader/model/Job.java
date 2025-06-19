package com.estuate.dicom_uploader.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Job {

    private String jobId;
    private String objectKey;        // File identifier
    private String platform;         // "gcp" or "azure"
    private String storageType;      // "native" or "blob"

    private JobStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;     // Only set in case of failure

    // Native (Healthcare API-specific) fields
    private String datasetName;
    private String dicomStoreName;

    // GCP Blob-specific fields
    private String bucketName;
    private String blobPath;

    // Azure Blob-specific fields
    private String blobContainer;
}
