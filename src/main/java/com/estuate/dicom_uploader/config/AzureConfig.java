package com.estuate.dicom_uploader.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "azure")
@Getter
@Setter
public class AzureConfig {
    private String dicomEndpoint;
    private String clientId;
    private String clientSecret;
    private String tenantId;
    private String scope;

}


