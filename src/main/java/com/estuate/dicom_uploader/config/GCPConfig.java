package com.estuate.dicom_uploader.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gcp")
@Getter
@Setter
public class GCPConfig {
    private String projectId;
    private String region;
    private String dataset;
    private String dicomStore;
}


