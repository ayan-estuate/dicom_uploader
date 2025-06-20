package com.estuate.dicom_uploader.dto;

import lombok.Data;

@Data
public class RetrieveRequest {
    private String studyInstanceUID;
    private String seriesInstanceUID;
    private String sopInstanceUID;
    private String platform; // "GCP" or "AZURE"
}
