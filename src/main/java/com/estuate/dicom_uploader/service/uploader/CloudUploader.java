package com.estuate.dicom_uploader.service.uploader;

import com.estuate.dicom_uploader.model.Job;
import java.io.File;

public interface CloudUploader {
    void upload(byte[] dicomData, Job job) throws Exception;
    boolean supports(String platform, String storageType);
}
