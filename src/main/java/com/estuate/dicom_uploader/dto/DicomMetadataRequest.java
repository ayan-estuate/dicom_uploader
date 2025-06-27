package com.estuate.dicom_uploader.dto;

import lombok.Data;

@Data
public class DicomMetadataRequest {
    private String platform;
    private String storageType;
    private String studyUid;
    private String projectId;
    private String location;
    private String dataset;
    private String dicomStore;
}
