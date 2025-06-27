package com.estuate.dicom_uploader.service;


import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

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


    public List<String> listAllKeysUnderPrefix(String prefix, String bucketName) {
        String resolvedBucket = (bucketName != null && !bucketName.isBlank()) ? bucketName : this.bucket;
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(resolvedBucket)
                .prefix(prefix)
                .build();

        return s3Client.listObjectsV2(listRequest)
                .contents()
                .stream()
                .map(S3Object::key)
                .filter(key -> !key.endsWith("/")) // Skip folders
                .collect(Collectors.toList());
    }



    public URL generatePresignedPutUrl(String objectKey) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType("application/dicom")
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .putObjectRequest(putRequest)
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .build();

        return presigner.presignPutObject(presignRequest).url();
    }
}

