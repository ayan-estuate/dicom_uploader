package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.config.AzureConfig;
import com.estuate.dicom_uploader.exception.DicomConflictException;
import com.estuate.dicom_uploader.exception.InvalidTokenException;
import com.estuate.dicom_uploader.exception.UploadFailedException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class AzureUploader {

    private static final Logger logger = LoggerFactory.getLogger(AzureUploader.class);

    private final AzureConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    public AzureUploader(AzureConfig config) {
        this.config = config;
    }

    public void uploadToAzure(byte[] dicomData) throws IOException {
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
                String sopInstanceUID = null;
                String studyUID = null;
                String seriesUID = null;

                try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomData))) {
                    Attributes attr = dis.readDataset(-1, -1);
                    sopInstanceUID = attr.getString(Tag.SOPInstanceUID);
                    studyUID = attr.getString(Tag.StudyInstanceUID);
                    seriesUID = attr.getString(Tag.SeriesInstanceUID);
                } catch (Exception e) {
                    logger.warn("Could not extract UIDs for conflict log", e);
                }

                throw new DicomConflictException(sopInstanceUID, studyUID, seriesUID);
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new UploadFailedException("Azure upload failed. HTTP Status: " + statusCode + ", Body: " + responseBody);
            }

            logger.info("Successfully uploaded DICOM to Azure. Status: {}", statusCode);

        } catch (IOException | ParseException e) {
            throw new UploadFailedException("IOException during Azure upload: " + e.getMessage());
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
