package com.estuate.dicom_uploader.controller;

import com.estuate.dicom_uploader.config.AzureConfig;
import com.estuate.dicom_uploader.dto.DicomMetadataRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/dicom")
@RequiredArgsConstructor
@Slf4j
public class DicomMetadataController {

    private final AzureConfig azureConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/metadata")
    public ResponseEntity<?> getDicomMetadata(@RequestBody DicomMetadataRequest request) {
        try {
            String platform = request.getPlatform();
            String storageType = request.getStorageType();
            String studyUid = request.getStudyUid();

            if ("gcp".equalsIgnoreCase(platform) && "native".equalsIgnoreCase(storageType)) {
                return fetchFromGcp(studyUid, request.getProjectId(), request.getLocation(), request.getDataset(), request.getDicomStore());
            } else if ("azure".equalsIgnoreCase(platform) && "native".equalsIgnoreCase(storageType)) {
                return fetchFromAzure(studyUid);
            } else {
                return ResponseEntity.badRequest().body("Unsupported platform or storageType.");
            }
        } catch (Exception e) {
            log.error("Error fetching metadata", e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    private ResponseEntity<?> fetchFromGcp(String studyUid, String projectId, String location, String dataset, String dicomStore) throws Exception {
        String baseUrl = String.format(
                "https://healthcare.googleapis.com/v1/projects/%s/locations/%s/datasets/%s/dicomStores/%s/dicomWeb",
                projectId, location, dataset, dicomStore
        );

        String accessToken = getGCPAccessToken();

        Map<String, Object> result = new HashMap<>();
        result.put("studyUid", studyUid);
        List<Map<String, Object>> seriesList = new ArrayList<>();

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String seriesUrl = baseUrl + "/studies/" + studyUid + "/series";
            HttpGet seriesGet = new HttpGet(seriesUrl);
            seriesGet.setHeader("Authorization", "Bearer " + accessToken);
            seriesGet.setHeader("Accept", "application/dicom+json");

            try (CloseableHttpResponse seriesRes = client.execute(seriesGet)) {
                String seriesJson = EntityUtils.toString(seriesRes.getEntity());
                JsonNode seriesArray = objectMapper.readTree(seriesJson);

                for (JsonNode seriesNode : seriesArray) {
                    JsonNode seriesUidNode = seriesNode.get("0020000E");
                    if (seriesUidNode == null || !seriesUidNode.has("Value")) continue;

                    String seriesUid = seriesUidNode.get("Value").get(0).asText();
                    List<String> instanceUids = new ArrayList<>();

                    String instanceUrl = baseUrl + "/studies/" + studyUid + "/series/" + seriesUid + "/instances";
                    HttpGet instanceGet = new HttpGet(instanceUrl);
                    instanceGet.setHeader("Authorization", "Bearer " + accessToken);
                    instanceGet.setHeader("Accept", "application/dicom+json");

                    try (CloseableHttpResponse instanceRes = client.execute(instanceGet)) {
                        String instanceJson = EntityUtils.toString(instanceRes.getEntity());
                        JsonNode instanceArray = objectMapper.readTree(instanceJson);

                        for (JsonNode inst : instanceArray) {
                            JsonNode sopInstanceUidNode = inst.get("00080018");
                            if (sopInstanceUidNode != null && sopInstanceUidNode.has("Value")) {
                                instanceUids.add(sopInstanceUidNode.get("Value").get(0).asText());
                            }
                        }
                    }

                    Map<String, Object> seriesEntry = new HashMap<>();
                    seriesEntry.put("seriesUid", seriesUid);
                    seriesEntry.put("instances", instanceUids);
                    seriesList.add(seriesEntry);
                }
            }
        }

        result.put("series", seriesList);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<?> fetchFromAzure(String studyUid) throws Exception {
        String url = azureConfig.getDicomEndpoint() + "/v1/partitions/default/studies/" + studyUid + "/metadata";

        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + getAzureToken());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(get)) {

            String body = EntityUtils.toString(response.getEntity());
            JsonNode array = objectMapper.readTree(body);

            Map<String, List<String>> seriesMap = new HashMap<>();

            for (JsonNode item : array) {
                String seriesUid = extractDicomValue(item, "0020000E");
                String instanceUid = extractDicomValue(item, "00080018");

                if (seriesUid != null && instanceUid != null) {
                    seriesMap.computeIfAbsent(seriesUid, k -> new ArrayList<>()).add(instanceUid);
                }
            }

            List<Map<String, Object>> seriesList = new ArrayList<>();
            for (Map.Entry<String, List<String>> entry : seriesMap.entrySet()) {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("seriesUid", entry.getKey());
                entryMap.put("instances", entry.getValue());
                seriesList.add(entryMap);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("studyUid", studyUid);
            result.put("series", seriesList);
            return ResponseEntity.ok(result);
        }
    }

    private String extractDicomValue(JsonNode node, String tag) {
        if (node.has(tag) && node.get(tag).has("Value")) {
            JsonNode valueArray = node.get(tag).get("Value");
            if (valueArray.isArray() && valueArray.size() > 0) {
                return valueArray.get(0).asText();
            }
        }
        return null;
    }

    private String getAzureToken() throws IOException, ParseException {
        String tokenUrl = "https://login.microsoftonline.com/" + azureConfig.getTenantId() + "/oauth2/v2.0/token";

        List<NameValuePair> params = Arrays.asList(
                new BasicNameValuePair("grant_type", "client_credentials"),
                new BasicNameValuePair("client_id", azureConfig.getClientId()),
                new BasicNameValuePair("client_secret", azureConfig.getClientSecret()),
                new BasicNameValuePair("scope", azureConfig.getScope())
        );

        HttpPost post = new HttpPost(tokenUrl);
        post.setHeader("Content-Type", "application/x-www-form-urlencoded");
        post.setEntity(new UrlEncodedFormEntity(params));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(post)) {

            String responseBody = EntityUtils.toString(response.getEntity());
            JsonNode json = objectMapper.readTree(responseBody);
            return json.get("access_token").asText();
        }
    }

    private String getGCPAccessToken() throws IOException {
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
