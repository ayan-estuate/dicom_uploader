package com.estuate.dicom_uploader.service;

import com.estuate.dicom_uploader.dto.RetrieveRequest;
import com.estuate.dicom_uploader.dto.RetrieveResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DicomRetrievalService {

    private final Map<String, CloudRetrievalStrategy> retrievalStrategies;
    private final S3PresignedUrlService s3Service;

    public RetrieveResponse retrieveAndStore(RetrieveRequest request) {
        CloudRetrievalStrategy strategy = retrievalStrategies.get(request.getPlatform().toUpperCase());
        if (strategy == null) throw new IllegalArgumentException("Invalid platform: " + request.getPlatform());

        try {
            byte[] dicomData = strategy.retrieveDicom(
                    request.getStudyInstanceUID(),
                    request.getSeriesInstanceUID(),
                    request.getSopInstanceUID()
            );

            String objectKey = String.format("%s/%s/%s.dcm",
                    request.getStudyInstanceUID(),
                    request.getSeriesInstanceUID(),
                    request.getSopInstanceUID());

            String url = s3Service.uploadToS3(objectKey, dicomData);
            return new RetrieveResponse("Success", url);

        } catch (Exception e) {
            return new RetrieveResponse("Failure: " + e.getMessage(), null);
        }
    }
}
