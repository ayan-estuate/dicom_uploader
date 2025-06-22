package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.uploader.CloudUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CloudUploaderFactory {

    private final List<CloudUploader> uploaders;

    public CloudUploader resolve(Job job) {
        return uploaders.stream()
                .filter(u -> u.supports(job.getPlatform(), job.getStorageType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No uploader found for platform=" + job.getPlatform() + " and storage=" + job.getStorageType()));
    }
}
