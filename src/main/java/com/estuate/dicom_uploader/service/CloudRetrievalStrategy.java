package com.estuate.dicom_uploader.service;

import java.io.IOException;

public interface CloudRetrievalStrategy {
    byte[] retrieveDicom(String studyInstanceUID,String seriesUID, String sopUID) throws IOException;
}
