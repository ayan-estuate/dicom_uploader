package com.estuate.dicom_uploader.service.azure;

import com.azure.storage.blob.*;
        import com.azure.storage.blob.models.*;
import com.estuate.dicom_uploader.config.AzureConfig;
import com.estuate.dicom_uploader.exception.InvalidTokenException;
import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.S3PresignedUrlService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;
import org.dcm4che3.data.Tag;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AzureRetrievalService {

    private final S3PresignedUrlService s3PresignedUrlService;
    private final AzureConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    public void retrieveFromGCPBlobAndUploadToS3(String connectionString, String containerName,
                                                   String blobName, String s3ObjectKey, Job job) {
        try {
            // Initialize Azure Blob client
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            if (!blobClient.exists()) {
                throw new FileNotFoundException("Blob not found in Azure Storage: " + blobName);
            }

            // Download blob data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blobClient.download(outputStream);
            byte[] dicomBytes = outputStream.toByteArray();

            log.info("📥 Retrieved blob from Azure Blob Storage: {}/{}", containerName, blobName);

            // Extract UIDs from DICOM
            try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomBytes))) {
                Attributes attr = dis.readDataset(-1, -1);
                job.setInstanceUid(attr.getString(Tag.SOPInstanceUID));
                job.setStudyUid(attr.getString(Tag.StudyInstanceUID));
                job.setSeriesUid(attr.getString(Tag.SeriesInstanceUID));
                log.info("📎 Extracted DICOM UIDs — SOP: {}, Study: {}, Series: {}",
                        job.getInstanceUid(), job.getStudyUid(), job.getSeriesUid());
            } catch (Exception e) {
                log.warn("⚠️ Failed to extract UIDs from blob", e);
            }

            // Upload to S3 using presigned URL
            URL s3PutUrl = s3PresignedUrlService.generatePresignedPutUrl(s3ObjectKey);
            uploadToS3ViaPresignedUrl(s3PutUrl, dicomBytes);

            log.info("✅ Uploaded blob to S3 as: {}", s3ObjectKey);

            // Delete from Azure Blob (optional cleanup)
            blobClient.delete();
            log.info("🗑 Deleted blob from Azure Blob Storage: {}", blobName);

        } catch (Exception e) {
            log.error("❌ Azure Blob to S3 transfer failed", e);
            throw new RuntimeException("Azure blob retrieval and S3 upload failed", e);
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
//    private void uploadToS3ViaPresignedUrl(URL presignedUrl, byte[] data) throws IOException {
//        HttpURLConnection connection = (HttpURLConnection) presignedUrl.openConnection();
//        connection.setDoOutput(true);
//        connection.setRequestMethod("PUT");
//       // connection.setRequestProperty("Content-Type", "application/octet-stream");
//
//        try (OutputStream out = connection.getOutputStream()) {
//            out.write(data);
//        }
//
//        int responseCode = connection.getResponseCode();
//        if (responseCode != 200) {
//            throw new IOException("Failed to upload to S3 via presigned URL. HTTP " + responseCode);
//        }
//    }


    public void retrieveNativeAndUploadToS3(String studyUID, String seriesUID, String sopUID) throws IOException {
        String url = config.getDicomEndpoint() + "/v1/partitions/default/studies/" +
                studyUID + "/series/" + seriesUID + "/instances/" + sopUID;
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + getAzureToken());
        get.setHeader("Accept", "application/dicom; transfer-syntax=*");

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(get)) {

            int status = response.getCode();
            if (status != 200) throw new IOException("Azure retrieve failed: " + status);

            byte[] data = EntityUtils.toByteArray(response.getEntity());

            // Upload to S3
            String s3ObjectKey = "dicom/" + studyUID + "/" + seriesUID + "/" + sopUID + ".dcm";
            URL presignedUrl = s3PresignedUrlService.generatePresignedPutUrl(s3ObjectKey);
            uploadToS3ViaPresignedUrl(presignedUrl, data);

            log.info("✅ Native DICOM uploaded to S3 at {}", s3ObjectKey);
        }
    }


    public String getAzureToken() throws IOException {
        String tokenUrl = "https://login.microsoftonline.com/" + config.getTenantId() + "/oauth2/v2.0/token";

        List<NameValuePair> params = Arrays.asList(
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("client_id", config.getClientId()),
                new BasicNameValuePair("client_secret", config.getClientSecret()),
                new BasicNameValuePair("scope", config.getScope())
        );

        HttpPost post = new HttpPost(tokenUrl);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new UrlEncodedFormEntity(params));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode < 200 || statusCode >= 300) {
                throw new InvalidTokenException("Failed to obtain Azure token. HTTP Status: " + statusCode + ", Body: " + responseBody);
            }

            Map<String, Object> result = mapper.readValue(responseBody, Map.class);
            String token = (String) result.get("access_token");

            if (token == null) {
                throw new InvalidTokenException("Access token not found in Azure response");
            }

            return token;
        } catch (ParseException e) {
            throw new InvalidTokenException("Failed to parse Azure token response: " + e.getMessage());
        }
    }

}
