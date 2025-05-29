package com.estuate.dicom_uploader.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;

    // Optional DICOM conflict details(json include going to handle if these fields are nulls)
    private final String sopInstanceUID;
    private final String studyInstanceUID;
    private final String seriesInstanceUID;
}
