package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.config.GCPConfig;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service("GCP")
@RequiredArgsConstructor
public class GCPRetriever implements CloudRetrievalStrategy {

    private final GCPConfig config;

    @Override
    public byte[] retrieveDicom(String studyUID, String seriesUID, String sopUID) throws IOException {
        // ✅ Construct the WADO-RS instance retrieval URL
        String uri = String.format(
                "https://healthcare.googleapis.com/v1/projects/%s/locations/%s/datasets/%s/dicomStores/%s/dicomWeb/studies/%s/series/%s/instances/%s",
                config.getProjectId(), config.getRegion(), config.getDataset(), config.getDicomStore(),
                studyUID, seriesUID, sopUID);

        // ✅ Authorize using ADC or service account
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        String token = credentials.getAccessToken().getTokenValue();

        // ✅ Build HTTP GET with correct Accept header
        HttpGet get = new HttpGet(uri);
        get.setHeader("Authorization", "Bearer " + getAccessToken());
        get.setHeader("Accept", "application/dicom; transfer-syntax=*");

        // ✅ Execute HTTP request
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(get)) {

            int status = response.getCode();
            if (status != 200) {
                throw new IOException("GCP DICOM retrieve failed: HTTP " + status);
            }

            // ✅ Extract bytes
            byte[] data = EntityUtils.toByteArray(response.getEntity());

            // ✅ Save locally for inspection (like Azure)
            Path folderPath = Paths.get("D:\\testdicom");
            if (Files.notExists(folderPath)) {
                Files.createDirectories(folderPath);
            }

            // Timestamped filename for uniqueness
            //String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            Path filePath = folderPath.resolve(String.format("%s_%s_%s.dcm", studyUID, seriesUID, sopUID));
            Files.write(filePath, data);

            return data;
        }
    }


    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();
        if (token == null) {
            throw new IOException("Failed to obtain access token");
        }
        return token.getTokenValue();
    }
}
