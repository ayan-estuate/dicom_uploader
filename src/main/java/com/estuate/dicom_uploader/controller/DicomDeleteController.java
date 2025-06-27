package com.estuate.dicom_uploader.controller;

import com.azure.storage.blob.*;
import com.estuate.dicom_uploader.config.AzureConfig;
import com.estuate.dicom_uploader.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dicom")
@RequiredArgsConstructor
@Slf4j
public class DicomDeleteController {

    private final AzureConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${azure.blob.connection-string}")
    private String connectionString;

    // 🔹 DELETE blob from blob storage
    @DeleteMapping("/blob")
    public ResponseEntity<?> deleteBlob(
            @RequestParam String containerName,
            @RequestParam String blobName) {
        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);
            if (!containerClient.exists()) return ResponseEntity.badRequest().body("Container does not exist.");

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            if (!blobClient.exists()) return ResponseEntity.badRequest().body("Blob not found.");

            blobClient.delete();
            return ResponseEntity.ok("Blob deleted successfully: " + blobName);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting blob: " + e.getMessage());
        }
    }

    // 🔹 DELETE single DICOM instance
    @DeleteMapping("/native")
    public ResponseEntity<?> deleteDicomInstance(
            @RequestParam String studyUid,
            @RequestParam String seriesUid,
            @RequestParam String instanceUid) {

        String url = config.getDicomEndpoint() +
                "/v1/partitions/default/studies/" + studyUid +
                "/series/" + seriesUid +
                "/instances/" + instanceUid;

        return executeNativeDelete(url, "Instance", instanceUid);
    }

    // 🔹 DELETE entire series
    @DeleteMapping("/native/series")
    public ResponseEntity<?> deleteDicomSeries(
            @RequestParam String studyUid,
            @RequestParam String seriesUid) {

        String url = config.getDicomEndpoint() +
                "/v1/partitions/default/studies/" + studyUid +
                "/series/" + seriesUid;

        return executeNativeDelete(url, "Series", seriesUid);
    }

    // 🔹 DELETE entire study
    @DeleteMapping("/native/study")
    public ResponseEntity<?> deleteDicomStudy(@RequestParam String studyUid) {
        String url = config.getDicomEndpoint() +
                "/v1/partitions/default/studies/" + studyUid;

        return executeNativeDelete(url, "Study", studyUid);
    }

    // 🔸 Shared native delete logic
    private ResponseEntity<?> executeNativeDelete(String url, String type, String uid) {
        HttpDelete delete = new HttpDelete(url);
        try {
            delete.setHeader("Authorization", "Bearer " + getAzureToken());

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(delete)) {

                int statusCode = response.getCode();
                String body = response.getEntity() != null ?
                        new String(response.getEntity().getContent().readAllBytes()) : "";

                if (statusCode >= 200 && statusCode < 300) {
                    log.info("✅ Deleted {} successfully — UID: {}", type, uid);
                    return ResponseEntity.ok(type + " deleted successfully.");
                } else {
                    log.warn("❌ Failed to delete {}. Status: {}, Body: {}", type, statusCode, body);
                    return ResponseEntity.status(statusCode)
                            .body("Failed to delete " + type + ". Status: " + statusCode + ". Body: " + body);
                }
            }
        } catch (Exception e) {
            log.error("Exception during DICOM {} delete", type, e);
            return ResponseEntity.internalServerError()
                    .body("Error deleting " + type + ": " + e.getMessage());
        }
    }

    // 🔸 Azure access token
    private String getAzureToken() throws IOException, ParseException {
        String tokenUrl = "https://login.microsoftonline.com/" + config.getTenantId() + "/oauth2/v2.0/token";

        List<NameValuePair> params = Arrays.asList(
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("client_id", config.getClientId()),
                new BasicNameValuePair("client_secret", config.getClientSecret()),
                new BasicNameValuePair("scope", config.getScope())
        );

        var post = new org.apache.hc.client5.http.classic.methods.HttpPost(tokenUrl);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new UrlEncodedFormEntity(params));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            int statusCode = response.getCode();
            String responseBody = new String(response.getEntity().getContent().readAllBytes());

            if (statusCode < 200 || statusCode >= 300) {
                throw new InvalidTokenException("Failed to get Azure token. Status: " + statusCode + ", Body: " + responseBody);
            }

            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
            String token = (String) result.get("access_token");

            if (token == null) {
                throw new InvalidTokenException("Access token not found in response.");
            }

            return token;
        }
    }
}
