package com.estuate.dicom_uploader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DicomUploaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(DicomUploaderApplication.class, args);
	}

}
