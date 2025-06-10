package com.estuate.dicom_uploader.dto;

// A small class to send response back after upload
public record UploadResponse(String status, String message, String jobId) {}