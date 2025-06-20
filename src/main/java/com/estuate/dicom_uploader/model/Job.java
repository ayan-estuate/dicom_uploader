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
    private JobType jobType;          // "upload", "retrieval", etc.
    private String objectKey;         // File identifier (e.g., S3 path or blob name)
    private String platform;          // "gcp" or "azure"
    private String storageType;       // "native" or "blob"

    private JobStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage;      // Only set in case of failure

    // Native (GCP Healthcare API-specific)
    private String datasetName;
    private String dicomStoreName;

    //Retrieval-specific fields(gcp-native)
    private String gcpProjectId;
    private String location;

    // Retrieval-specific fields (optional, used for DICOM UIDs)`
    private String studyUid;
    private String seriesUid;
    private String instanceUid;

    // GCP Blob-specific fields
    private String bucketName;
    private String blobPath;

    // Azure Blob-specific fields
    private String blobContainer;
}
