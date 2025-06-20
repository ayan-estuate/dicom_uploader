package com.estuate.dicom_uploader.controller;

import com.azure.storage.blob.*;
import com.estuate.dicom_uploader.config.AzureConfig;
import com.estuate.dicom_uploader.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.message.BasicNameValuePair;

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

    @DeleteMapping("/blob")
    public ResponseEntity<?> deleteBlob(
            @RequestParam String containerName,
            @RequestParam String blobName) {

        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                return ResponseEntity.badRequest().body("Container does not exist.");
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            if (!blobClient.exists()) {
                return ResponseEntity.badRequest().body("Blob not found.");
            }

            blobClient.delete();
            return ResponseEntity.ok("Blob deleted successfully: " + blobName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting blob: " + e.getMessage());
        }
    }

    @DeleteMapping("/native")
    public ResponseEntity<?> deleteDicomNative(
            @RequestParam String studyUid,
            @RequestParam String seriesUid,
            @RequestParam String instanceUid) {

        String url = config.getDicomEndpoint() +
                "/v1/partitions/default/studies/" + studyUid +
                "/series/" + seriesUid +
                "/instances/" + instanceUid;

        HttpDelete delete = new HttpDelete(url);
        try {
            delete.setHeader("Authorization", "Bearer " + getAzureToken());

            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(delete)) {

                int statusCode = response.getCode();
                if (statusCode >= 200 && statusCode < 300) {
                    log.info("✅ Deleted DICOM instance successfully — Study: {}, Series: {}, Instance: {}", studyUid, seriesUid, instanceUid);
                    return ResponseEntity.ok("DICOM instance deleted successfully.");
                } else {
                    String body = response.getEntity() != null ? new String(response.getEntity().getContent().readAllBytes()) : "";
                    log.warn("❌ Failed to delete DICOM. Status: {}, Body: {}", statusCode, body);
                    return ResponseEntity.status(statusCode).body("Failed to delete DICOM. Status: " + statusCode + ". Body: " + body);
                }
            }
        } catch (Exception e) {
            log.error("Exception during native DICOM delete", e);
            return ResponseEntity.internalServerError().body("Error deleting DICOM: " + e.getMessage());
        }
    }

    // Utility: Azure access token (same as used elsewhere)
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
