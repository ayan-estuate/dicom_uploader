package com.estuate.dicom_uploader.service;

import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.*;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

@RequiredArgsConstructor
@Slf4j
@Service
public class DicomRetrievalService {

    private final S3PresignedUrlService s3PresignedUrlService;

    public void retrieveAndUploadToS3(String projectId, String location, String dataset,
                                      String dicomStore, String studyUid, String seriesUid, String instanceUid,
                                      String objectKey) {

        String url = String.format(
                "https://healthcare.googleapis.com/v1/projects/%s/locations/%s/datasets/%s/dicomStores/%s/dicomWeb/studies/%s/series/%s/instances/%s",
                projectId, location, dataset, dicomStore, studyUid, seriesUid, instanceUid
        );

        try {
            String token = getAccessToken();

            // Step 1: Retrieve from GCP
            byte[] dicomBytes = Request.get(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Accept", "application/dicom")
                    .execute()
                    .returnContent()
                    .asBytes();

            // Step 2: Upload to S3
            URL putUrl = s3PresignedUrlService.generatePresignedPutUrl(objectKey);
            uploadToS3ViaPresignedUrl(putUrl, dicomBytes);

            // Step 3: Log
            log.info("📌 Retrieved DICOM Study UID: {}", studyUid);
            log.info("📦 Uploaded to S3 key: {}", objectKey);

            // Step 4: Delete from GCP after successful upload
            deleteFromGCP(projectId, location, dataset, dicomStore, studyUid, seriesUid, instanceUid, token);

        } catch (Exception e) {
            log.error("❌ Retrieval or S3 upload/delete failed", e);
            throw new RuntimeException("DICOM round-trip failed", e);
        }
    }

    public void retrieveFromGCPBlobAndUploadToS3(String gcpProjectId, String bucketName, String blobName, String s3ObjectKey) {
        try {
            // Step 1: Get blob from GCP
            Storage storage = StorageOptions.newBuilder()
                    .setProjectId(gcpProjectId)
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build()
                    .getService();

            Blob blob = storage.get(BlobId.of(bucketName, blobName));
            if (blob == null || !blob.exists()) {
                throw new IOException("❌ Blob not found in GCP: " + blobName);
            }

            byte[] data = blob.getContent();
            log.info("✅ Retrieved blob from GCP Cloud Storage: gs://{}/{}", bucketName, blobName);

            // Step 2: Upload to S3 via presigned URL
            URL s3PutUrl = s3PresignedUrlService.generatePresignedPutUrl(s3ObjectKey);
            uploadToS3ViaPresignedUrl(s3PutUrl, data);
            log.info("📦 Uploaded blob to S3 at key: {}", s3ObjectKey);

            // Step 3: Log metadata
            log.info("🧾 Roundtrip complete. Blob '{}' from bucket '{}' uploaded to S3 key '{}'", blobName, bucketName, s3ObjectKey);

            // Step 4: Optional delete from GCP
            boolean deleted = storage.delete(BlobId.of(bucketName, blobName));
            if (deleted) {
                log.info("🗑️ Deleted blob from GCP: {}", blobName);
            } else {
                log.warn("⚠️ Failed to delete blob from GCP or it didn't exist: {}", blobName);
            }

        } catch (Exception e) {
            log.error("❌ Failed roundtrip from GCP Blob to S3", e);
            throw new RuntimeException("Blob roundtrip failed", e);
        }
    }

    private void uploadToS3ViaPresignedUrl(URL putUrl, byte[] data) throws IOException {
        HttpPut put = new HttpPut(putUrl.toString());
        put.setHeader("Content-Type", "application/dicom");
        put.setEntity(new ByteArrayEntity(data, ContentType.APPLICATION_OCTET_STREAM));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(put)) {

            int status = response.getCode();
            if (status != 200 && status != 201) {
                throw new IOException("Failed to upload to S3 via presigned URL, HTTP status: " + status);
            }

            log.info("✅ Uploaded to S3 using presigned PUT URL");
        }
    }

    private void deleteFromGCP(String projectId, String location, String dataset,
                               String dicomStore, String studyUid, String seriesUid,
                               String instanceUid, String accessToken) throws IOException {

        String deleteUrl = String.format(
                "https://healthcare.googleapis.com/v1/projects/%s/locations/%s/datasets/%s/dicomStores/%s/dicomWeb/studies/%s/series/%s/instances/%s",
                projectId, location, dataset, dicomStore, studyUid, seriesUid, instanceUid
        );

        HttpDelete delete = new HttpDelete(deleteUrl);
        delete.setHeader("Authorization", "Bearer " + accessToken);

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(delete)) {

            int status = response.getCode();
            if (status != 200 && status != 204) {
                throw new IOException("Failed to delete from GCP Healthcare API, HTTP status: " + status);
            }

            log.info("🗑️ Successfully deleted instance from GCP DICOM store");
        }
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}
