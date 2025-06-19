package com.estuate.dicom_uploader.controller;

import com.azure.storage.blob.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dicom")
@RequiredArgsConstructor
public class DicomDeleteController {

    @Value("${azure.blob.connection-string}")
    private String connectionString;

    @DeleteMapping("/blob")
    public ResponseEntity<?> deleteBlob(
            @RequestParam String containerName,
            @RequestParam String blobName) {

        try {
            BlobServiceClient serviceClient = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();

            BlobContainerClient containerClient = serviceClient.getBlobContainerClient(containerName);

            if (!containerClient.exists()) {
                return ResponseEntity.badRequest().body("Container does not exist.");
            }

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            if (!blobClient.exists()) {
                return ResponseEntity.badRequest().body("Blob not found.");
            }

            blobClient.delete();
            return ResponseEntity.ok("Blob deleted successfully: " + blobName);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error deleting blob: " + e.getMessage());
        }
    }
}
