package com.estuate.dicom_uploader.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class DownloaderService {

    private final CloseableHttpClient httpClient = HttpClients.custom().build();
    @Retryable(
            value = { IOException.class, javax.net.ssl.SSLException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 3000, multiplier = 2)
    )
    public File downloadToTempFile(String url, String jobId) throws IOException {
        HttpGet request = new HttpGet(url);
        try (var response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            if (entity == null || entity.getContentLength() <= 0) {
                throw new IOException("Empty or invalid content in response.");
            }

//            Path tempFilePath = Files.createTempFile("dicom_" + jobId + "_" + UUID.randomUUID(), ".dcm");
            Path tempFilePath = Files.createTempFile(Paths.get(System.getProperty("user.dir"), "jobs", "temp"), "dicom_" + jobId + "_", ".dcm");

            File tempFile = tempFilePath.toFile();

            try (InputStream in = entity.getContent();
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                in.transferTo(out);
            }
            System.out.println(tempFile.getAbsolutePath());

            log.info("Downloaded DICOM to temp file for job {}", jobId);
            return tempFile;
        }
    }
}
