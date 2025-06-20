package com.estuate.dicom_uploader.controller;

import com.estuate.dicom_uploader.dto.RetrieveRequest;
import com.estuate.dicom_uploader.dto.RetrieveResponse;
import com.estuate.dicom_uploader.service.DicomRetrievalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dicom/retrieve")
@RequiredArgsConstructor
public class DicomRetrievalController {

    private final DicomRetrievalService retrievalService;

    @PostMapping
    public ResponseEntity<RetrieveResponse> retrieveDicom(@RequestBody RetrieveRequest request) {
        RetrieveResponse response = retrievalService.retrieveAndStore(request);
        return ResponseEntity.ok(response);
    }
}
