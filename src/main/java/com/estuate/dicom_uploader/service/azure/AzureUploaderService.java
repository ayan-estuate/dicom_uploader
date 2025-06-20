package com.estuate.dicom_uploader.service.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.estuate.dicom_uploader.config.AzureConfig;
import com.estuate.dicom_uploader.exception.DicomConflictException;
import com.estuate.dicom_uploader.exception.InvalidTokenException;
import com.estuate.dicom_uploader.exception.UploadFailedException;
import com.estuate.dicom_uploader.model.Job;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
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
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AzureUploaderService {

    private static final Logger logger = LoggerFactory.getLogger(AzureUploaderService.class);

    private final AzureConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${azure.blob.connection-string}")
    private String connectionString;

    public AzureUploaderService(AzureConfig config) {
        this.config = config;
    }

    public void uploadToAzure(byte[] dicomData, Job job) throws IOException {
        // Extract UIDs before upload attempt
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomData))) {
            Attributes attr = dis.readDataset(-1, -1);
            job.setInstanceUid(attr.getString(Tag.SOPInstanceUID));
            job.setStudyUid(attr.getString(Tag.StudyInstanceUID));
            job.setSeriesUid(attr.getString(Tag.SeriesInstanceUID));
            logger.info("Extracted DICOM UIDs — SOP: {}, Study: {}, Series: {}",
                    job.getInstanceUid(), job.getStudyUid(), job.getSeriesUid());
        } catch (Exception e) {
            logger.warn("⚠️ Failed to extract DICOM UIDs before Azure upload", e);
        }

        String url = config.getDicomEndpoint() + "/v1/partitions/default/studies";
        logger.info("Uploading DICOM to Azure at URL: {}", url);

        HttpPost post = new HttpPost(url);
        post.setHeader("Authorization", "Bearer " + getAzureToken());
        post.setHeader("Content-Type", "application/dicom");
        post.setHeader("Accept", "application/dicom+json");
        post.setEntity(new ByteArrayEntity(dicomData, ContentType.create("application/dicom")));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            logger.info("Azure upload response: HTTP {}, Body: {}", statusCode, responseBody);

            if (statusCode == 409) {
                throw new DicomConflictException(job.getInstanceUid(), job.getStudyUid(), job.getSeriesUid());
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new UploadFailedException("Azure upload failed. HTTP Status: " + statusCode + ", Body: " + responseBody);
            }

            logger.info("✅ Successfully uploaded DICOM to Azure. Status: {}", statusCode);

        } catch (IOException | ParseException e) {
            throw new UploadFailedException("IOException during Azure upload: " + e.getMessage());
        }
    }


    public void uploadToAzureBlob(byte[] fileData, Job job) throws IOException {
        try {
            // Initialize the container client
            BlobContainerClient containerClient = new BlobContainerClientBuilder()
                    .connectionString(connectionString)
                    .containerName(job.getBlobContainer())
                    .buildClient();

            // Create the container if it doesn't exist
            if (!containerClient.exists()) {
                log.info("Container '{}' not found. Creating it...", job.getBlobContainer());
                containerClient.create();
            }
            String objectFileName = Paths.get(job.getObjectKey()).getFileName().toString();

            String blobName;
            if (job.getBlobPath() != null && !job.getBlobPath().isBlank()) {
                blobName = job.getBlobPath().replaceAll("/+$", "") + "/" + objectFileName;
            } else {
                blobName = objectFileName;
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);


            // Upload the file
            blobClient.upload(new ByteArrayInputStream(fileData), fileData.length, true);

            log.info("File uploaded successfully to Azure Blob as '{}'", blobName);

        } catch (Exception e) {
            log.error("Failed to upload file to Azure Blob Storage", e);
            throw new IOException("Azure Blob upload failed", e);
        }
    }


    private String getAzureToken() throws IOException {
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
