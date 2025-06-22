package com.estuate.dicom_uploader.service.gcp;

import com.estuate.dicom_uploader.config.GCPConfig;
import com.estuate.dicom_uploader.exception.DicomConflictException;
import com.estuate.dicom_uploader.exception.DicomUploadException;
import com.estuate.dicom_uploader.model.Job;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GCPUploaderService {

    private final GCPConfig config;
    private final ExecutorService bucketExecutor;

    public void uploadToGCP(byte[] dicomData, Job job) throws IOException {
        // Extract UIDs and store in Job
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomData))) {
            Attributes attr = dis.readDataset(-1, -1);
            job.setInstanceUid(attr.getString(Tag.SOPInstanceUID));
            job.setStudyUid(attr.getString(Tag.StudyInstanceUID));
            job.setSeriesUid(attr.getString(Tag.SeriesInstanceUID));
            log.info("Extracted DICOM UIDs — SOP: {}, Study: {}, Series: {}", job.getInstanceUid(), job.getStudyUid(), job.getSeriesUid());
        } catch (Exception e) {
            log.warn("⚠️ Failed to extract DICOM UIDs for job {}", job.getJobId(), e);
        }

        String dicomUri = String.format(
                "https://healthcare.googleapis.com/v1/projects/%s/locations/%s/datasets/%s/dicomStores/%s/dicomWeb/studies",
                config.getProjectId(), config.getRegion(), job.getDatasetName(), job.getDicomStoreName());

        String accessToken = getAccessToken();

        HttpPost post = new HttpPost(dicomUri);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/dicom");
        post.setEntity(new ByteArrayEntity(dicomData, ContentType.create("application/dicom")));

        log.info("Uploading to GCP Healthcare API: {}", dicomUri);
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(post)) {

            int statusCode = response.getCode();

            if (statusCode == 409) {
                throw new DicomConflictException(job.getInstanceUid(), job.getStudyUid(), job.getSeriesUid());
            }

            if (statusCode != 200 && statusCode != 202) {
                throw new DicomUploadException("Unexpected response from GCP Healthcare API: HTTP " + statusCode);
            }

            log.info("✅ Successfully uploaded to GCP Healthcare API: HTTP {}", statusCode);

        } catch (IOException ex) {
            throw new DicomUploadException("Failed to upload DICOM to GCP", ex);
        }
    }


    public void uploadToGCPBlob(byte[] fileData, Job job) throws IOException {
        Storage storage = StorageOptions.newBuilder()
                .setProjectId(config.getProjectId())
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .build()
                .getService();

        log.info("Using GCP Project: {}", storage.getOptions().getProjectId());

        String bucketName = job.getBucketName();
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("Bucket name is required for GCP blob upload.");
        }

        Bucket bucket = safelyFetchBucket(storage, bucketName, 5, TimeUnit.SECONDS);
        if (bucket == null) {
            throw new IOException("Bucket '" + bucketName + "' not found. Ensure it's created manually in GCP.");
        }

        String fileName = Paths.get(job.getObjectKey()).getFileName().toString();
        String blobPath = (job.getBlobPath() != null && !job.getBlobPath().isBlank())
                ? job.getBlobPath().replaceAll("/+$", "") + "/" + fileName
                : fileName;

        BlobId blobId = BlobId.of(bucketName, blobPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/dicom")
                .build();

        storage.create(blobInfo, fileData);

        log.info("✅ Uploaded DICOM to GCP Blob Storage at '{}'", blobPath);
    }

    private Bucket safelyFetchBucket(Storage storage, String bucketName, long timeout, TimeUnit unit) {
        try {
            Future<Bucket> future = bucketExecutor.submit(() -> storage.get(bucketName));
            return future.get(timeout, unit);
        } catch (TimeoutException te) {
            log.error("❌ Timeout fetching bucket '{}'", bucketName);
        } catch (Exception e) {
            log.error("❌ Error fetching bucket '{}'", bucketName, e);
        }
        return null;
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
