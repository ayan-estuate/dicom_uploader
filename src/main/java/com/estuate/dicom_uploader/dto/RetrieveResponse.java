package com.estuate.dicom_uploader.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RetrieveResponse {
    private String message;
    private String s3Url;
}
