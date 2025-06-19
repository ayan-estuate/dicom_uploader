package com.estuate.dicom_uploader.dto;

import lombok.Data;

@Data
public class DicomRetrievalRequest {
    private String projectId;
    private String location;
    private String dataset;
    private String dicomStore;
    private String studyUid;
    private String seriesUid;
    private String instanceUid;
    private String objectKey;
}
