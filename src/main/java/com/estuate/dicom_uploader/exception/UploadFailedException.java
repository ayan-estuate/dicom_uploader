package com.estuate.dicom_uploader.exception;

public class UploadFailedException extends RuntimeException {
    public UploadFailedException(String message) {
        super(message);
    }
}
