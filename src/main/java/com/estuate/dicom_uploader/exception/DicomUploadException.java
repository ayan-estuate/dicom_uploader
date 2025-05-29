package com.estuate.dicom_uploader.exception;

public class DicomUploadException extends RuntimeException {
    public DicomUploadException(String message) {
        super(message);
    }

    public DicomUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}


