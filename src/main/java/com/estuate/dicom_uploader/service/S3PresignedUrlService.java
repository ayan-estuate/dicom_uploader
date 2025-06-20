package com.estuate.dicom_uploader.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3PresignedUrlService {

    private final S3Presigner presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.presigned.expiration}")
    private int expirationMinutes;

    public URL generatePresignedUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .getObjectRequest(getObjectRequest)
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .build();

        return presigner.presignGetObject(presignRequest).url();
    }

    public String uploadToS3(String objectKey, byte[] data) {
        // Prefix the object key with your folder name
        String folder = "retrieved-dicom-images/";
        String fullObjectKey = folder + objectKey;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fullObjectKey)
                .contentType("application/dicom")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(data));

        return String.format("https://%s.s3.amazonaws.com/%s", bucket, fullObjectKey);
    }

}

