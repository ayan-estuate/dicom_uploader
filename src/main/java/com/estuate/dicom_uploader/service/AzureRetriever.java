package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.config.AzureConfig;
import com.estuate.dicom_uploader.exception.InvalidTokenException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service("AZURE")
@RequiredArgsConstructor
public class AzureRetriever implements CloudRetrievalStrategy {

    private final AzureConfig config;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public byte[] retrieveDicom(String studyUID,String seriesUID, String sopUID) throws IOException {
        //String token = // logic to get Azure token;
        String url = config.getDicomEndpoint() + "/v1/partitions/default/studies/" +
                studyUID + "/series/" + seriesUID + "/instances/" + sopUID;
        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + getAzureToken());
        get.setHeader("Accept", "application/dicom; transfer-syntax=*");


        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(get)) {

            int status = response.getCode();
            if (status != 200) throw new IOException("Azure retrieve failed: " + status);
            // ✅ Convert response entity to byte array
            byte[] data = EntityUtils.toByteArray(response.getEntity());

            // ✅ Write to disk for debugging/viewing in DICOM viewer
            Path folderPath = Paths.get("D:\\testdicom");
            if (Files.notExists(folderPath)) {
                Files.createDirectories(folderPath);
            }
            Files.write(folderPath.resolve("test-output.dcm"), data);  // <- Add this line here

            return data;
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
