package com.estuate.dicom_uploader.controller;

import com.estuate.dicom_uploader.model.Job;
import com.estuate.dicom_uploader.service.DicomRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dicom-job")
public class DicomRetrievalController {

    private final DicomRetrievalService retrievalService;

    @PostMapping("/retrieve")
    public ResponseEntity<String> retrieveDicom(@RequestBody Job job) {
        try {
            retrievalService.retrieveAndUpload(job);
            return ResponseEntity.ok("DICOM retrieval and upload successful.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve and upload DICOM: " + e.getMessage());
        }
    }
}
