package com.estuate.dicom_uploader.exception;

public class DicomConflictException extends DicomUploadException {

    private final String sopInstanceUID;
    private final String studyInstanceUID;
    private final String seriesInstanceUID;

    public DicomConflictException(String sopInstanceUID, String studyUID, String seriesUID) {
        super(String.format("Duplicate DICOM detected — SOPInstanceUID: %s, StudyInstanceUID: %s, SeriesInstanceUID: %s",
                sopInstanceUID, studyUID, seriesUID));
        this.sopInstanceUID = sopInstanceUID;
        this.studyInstanceUID = studyUID;
        this.seriesInstanceUID = seriesUID;
    }

    public String getSopInstanceUID() {
        return sopInstanceUID;
    }

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public String getSeriesInstanceUID() {
        return seriesInstanceUID;
    }
}
