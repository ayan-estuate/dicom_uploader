package com.estuate.dicom_uploader.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DicomUploaderService {


    private final GCPUploader gcpUploader;
    private final AzureUploader azureUploader;

    public void upload(String presignedUrl, String platform) throws IOException, ParseException {
        byte[] dicomData = downloadDicomFile(presignedUrl);

        switch (platform.toLowerCase()) {
            case "gcp" -> gcpUploader.uploadToGCP(dicomData);
            case "azure" -> azureUploader.uploadToAzure(dicomData);
            default -> throw new IllegalArgumentException("Invalid platform: " + platform);
        }


    }

    private byte[] downloadDicomFile(String url) throws IOException {
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
            log.error("Error downloading DICOM file from URL: {}", url, e);
            throw new IOException("Error downloading DICOM file from " + url, e);
        }
    }
}
