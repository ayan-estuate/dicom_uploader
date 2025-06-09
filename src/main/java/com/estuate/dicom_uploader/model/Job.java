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
    private String objectKey;    // S3 object key
    private String platform;     // "gcp" or "azure"
    private JobStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String errorMessage; // Only set in case of failure
}
