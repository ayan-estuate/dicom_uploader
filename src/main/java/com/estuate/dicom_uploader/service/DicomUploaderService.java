package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.azure.AzureUploaderService;
import com.estuate.dicom_uploader.service.gcp.GCPUploaderService;
import com.estuate.dicom_uploader.util.DicomValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DicomUploaderService {

    private final GCPUploaderService gcpUploaderService;
    private final AzureUploaderService azureUploaderService;

    public void upload(String presignedUrl, Job job) throws IOException, ParseException {
        byte[] fileData = downloadFile(presignedUrl);

        String platform = job.getPlatform();
        String storageType = job.getStorageType();

        // Native storage must have valid DICOM
        if ("native".equalsIgnoreCase(storageType) && !DicomValidator.isValidDicom(fileData)) {
            throw new IllegalArgumentException("Uploaded file is not a valid DICOM, but storage type is set to native.");
        }

        switch (platform.toLowerCase()) {
            case "gcp" -> {
                if ("blob".equalsIgnoreCase(storageType)) {
                    gcpUploaderService.uploadToGCPBlob(fileData, job);
                } else {
                    gcpUploaderService.uploadToGCP(fileData, job);
                }
            }
            case "azure" -> {
                if ("blob".equalsIgnoreCase(storageType)) {
                    azureUploaderService.uploadToAzureBlob(fileData, job);
                } else {
                    azureUploaderService.uploadToAzure(fileData, job);
                }
            }
            default -> throw new IllegalArgumentException("Invalid platform: " + platform);
        }
    }

    private byte[] downloadFile(String url) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                if (statusCode >= 200 && statusCode < 300) {
                    return EntityUtils.toByteArray(response.getEntity());
                } else {
                    throw new IOException("Failed to download file. HTTP status: " + statusCode);
                }
            }
        } catch (Exception e) {
            log.error("Error downloading file from URL: {}", url, e);
            throw new IOException("Error downloading file from " + url, e);
        }
    }
}
