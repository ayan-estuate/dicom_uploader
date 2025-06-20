//package com.estuate.dicom_uploader.controller;
//
//import com.estuate.dicom_uploader.dto.DicomBlobRetrievalRequest;
//import com.estuate.dicom_uploader.dto.DicomRetrievalRequest;
//import com.estuate.dicom_uploader.service.DicomRetrievalService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/dicom/retrieve")
//@RequiredArgsConstructor
//public class DicomRetrievalController {
//
//    private final DicomRetrievalService retrievalService;
//
//    @PostMapping
//    public ResponseEntity<String> retrieveAndStore(@RequestBody DicomRetrievalRequest request) {
//        retrievalService.retrieveGCPNativeAndUploadToS3(
//                request.getProjectId(),
//                request.getLocation(),
//                request.getDataset(),
//                request.getDicomStore(),
//                request.getStudyUid(),
//                request.getSeriesUid(),
//                request.getInstanceUid(),
//                request.getObjectKey()
//        );
//        return ResponseEntity.ok("✅ Retrieved from GCP and uploaded to S3 at: " + request.getObjectKey());
//    }
//
//    @PostMapping("/blob")
//    public ResponseEntity<String> retrieveFromBlobAndStoreToS3(@RequestBody DicomBlobRetrievalRequest request) {
//        retrievalService.retrieveFromGCPBlobAndUploadToS3(
//                request.getProjectId(),
//                request.getBucket(),
//                request.getBlobPath(),
//                request.getObjectKey()
//        );
//
//        return ResponseEntity.ok("✅ Blob '" + request.getBlobPath() + "' from bucket '" +
//                request.getBucket() + "' uploaded to S3 at key: " + request.getObjectKey());
//    }
//}
