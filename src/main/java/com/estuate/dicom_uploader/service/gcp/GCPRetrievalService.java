package com.estuate.dicom_uploader.service.gcp;

import com.estuate.dicom_uploader.service.S3PresignedUrlService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
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

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GCPRetrievalService {

    private final S3PresignedUrlService s3PresignedUrlService;

    public void retrieveNativeAndUploadToS3(String projectId, String location, String dataset,
                                            String dicomStore, String studyUid, String seriesUid, String instanceUid,
                                            String objectKey) {
        String url = String.format(
                "https://healthcare.googleapis.com/v1/projects/%s/locations/%s/datasets/%s/dicomStores/%s/dicomWeb/studies/%s/series/%s/instances/%s",
                projectId, location, dataset, dicomStore, studyUid, seriesUid, instanceUid
        );


        try {
            String token = getAccessToken();

            // Step 1: Download from GCP native
            byte[] dicomBytes = Request.get(url)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Accept", "application/dicom")
                    .execute()
                    .returnContent()
                    .asBytes();

            // Step 2: Upload to S3
            URL putUrl = s3PresignedUrlService.generatePresignedPutUrl(objectKey);
            uploadToS3ViaPresignedUrl(putUrl, dicomBytes);

            log.info("✅ GCP native retrieval roundtrip complete for instance {}", instanceUid);

            // Step 3: Cleanup
            deleteFromGCP(projectId, location, dataset, dicomStore, studyUid, seriesUid, instanceUid, token);

        } catch (Exception e) {
            log.error("❌ GCP native roundtrip failed", e);
            throw new RuntimeException("DICOM round-trip failed", e);
        }
    }

    public void retrieveFromGCPBlobAndUploadToS3(String gcpProjectId, String bucketName, String blobName, String s3ObjectKey) {
        try {
            Storage storage = StorageOptions.newBuilder()
                    .setProjectId(gcpProjectId)
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build()
                    .getService();

            Blob blob = storage.get(BlobId.of(bucketName, blobName));
            if (blob == null || !blob.exists()) {
                throw new IOException("Blob not found in GCP: " + blobName);
            }

            byte[] data = blob.getContent();
            log.info("📦 Retrieved blob from GCP Storage: gs://{}/{}", bucketName, blobName);

            URL s3PutUrl = s3PresignedUrlService.generatePresignedPutUrl(s3ObjectKey);
            uploadToS3ViaPresignedUrl(s3PutUrl, data);

            log.info("✅ Uploaded blob to S3: {}", s3ObjectKey);

            boolean deleted = storage.delete(BlobId.of(bucketName, blobName));
            if (deleted) {
                log.info("🗑 Deleted blob from GCP: {}", blobName);
            }

        } catch (Exception e) {
            log.error("❌ GCP blob roundtrip failed", e);
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
                throw new IOException("Upload to S3 failed, HTTP status: " + status);
            }

            log.info("✅ Upload to S3 via presigned URL complete");
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
                throw new IOException("GCP delete failed, HTTP status: " + status);
            }

            log.info("✅ Deleted instance from GCP store");
        }
    }

    private String getAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped(Collections.singletonList("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        return credentials.getAccessToken().getTokenValue();
    }
}
