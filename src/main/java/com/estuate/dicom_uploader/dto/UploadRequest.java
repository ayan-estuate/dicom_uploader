package com.estuate.dicom_uploader.dto;

import jakarta.validation.constraints.NotBlank;

// A small class to accept upload requests. It requires objectKey and platform.
public record UploadRequest(@NotBlank String objectKey, @NotBlank String platform) {}
