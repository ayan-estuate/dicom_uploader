package com.estuate.dicom_uploader.controller;



import com.estuate.dicom_uploader.service.DicomUploaderService;
import com.estuate.dicom_uploader.service.S3PresignedUrlService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

@RestController
@RequestMapping("/dicom")
@RequiredArgsConstructor
@Validated
@Slf4j
public class DicomController {

    private final DicomUploaderService dicomUploaderService;
    private final S3PresignedUrlService s3PresignedUrlService;

    private static final Set<String> SUPPORTED_PLATFORMS = Set.of("gcp", "azure");

    public record UploadRequest(@NotBlank String objectKey, @NotBlank String platform) {}
    public record UploadResponse(String status, String message) {}

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDicom(@RequestBody @Valid UploadRequest request) throws IOException, ParseException {
        String platformLower = request.platform().toLowerCase();

        if (!SUPPORTED_PLATFORMS.contains(platformLower)) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse("error", "Unsupported platform: " + request.platform()));
        }

        URL presignedUrl = s3PresignedUrlService.generatePresignedUrl(request.objectKey());
        dicomUploaderService.upload(presignedUrl.toString(), platformLower);

        return ResponseEntity.ok(new UploadResponse("success", "DICOM uploaded to " + platformLower));
    }
}
