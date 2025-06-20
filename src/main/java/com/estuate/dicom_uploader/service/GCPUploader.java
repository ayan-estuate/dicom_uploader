package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.config.GCPConfig;
import com.estuate.dicom_uploader.exception.DicomConflictException;
import com.estuate.dicom_uploader.exception.DicomUploadException;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
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
import java.io.IOException;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class GCPUploader {

    private final GCPConfig config;

    public void uploadToGCP(byte[] dicomData) throws IOException {
        String dicomUri = String.format(
                "https://healthcare.googleapis.com/v1/projects/%s/locations/%s/datasets/%s/dicomStores/%s/dicomWeb/studies",
                config.getProjectId(), config.getRegion(), config.getDataset(), config.getDicomStore());

        String accessToken = getAccessToken();

        HttpPost post = new HttpPost(dicomUri);
        post.setHeader("Authorization", "Bearer " + accessToken);
        post.setHeader("Content-Type", "application/dicom");
        post.setEntity(new ByteArrayEntity(dicomData, ContentType.create("application/dicom")));
        log.info("before job:{}",System.currentTimeMillis());
        try (CloseableHttpClient httpClient = HttpClients.createDefault();

             CloseableHttpResponse response =  httpClient.execute(post)) {
            log.info("Uploading DICOM to GCP: {}", dicomUri);
            int statusCode = response.getCode();

            if (statusCode == 409) {
                String sopInstanceUID = null;
                String studyUID = null;
                String seriesUID = null;
                try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomData))) {
                    Attributes attr = dis.readDataset(-1, -1);
                    sopInstanceUID = attr.getString(Tag.SOPInstanceUID);
                    studyUID = attr.getString(Tag.StudyInstanceUID);
                    seriesUID = attr.getString(Tag.SeriesInstanceUID);
                } catch (Exception e) {
                    log.warn("Could not extract UIDs from DICOM for conflict analysis", e);
                }
                throw new DicomConflictException(sopInstanceUID, studyUID, seriesUID);
            }

            if (statusCode != 200 && statusCode != 202) {
                throw new DicomUploadException("Unexpected response from GCP: HTTP " + statusCode);
            }

            log.info("Successfully uploaded DICOM. HTTP {}", statusCode);

        } catch (IOException ex) {
            throw new DicomUploadException("Failed to upload DICOM to GCP", ex);
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