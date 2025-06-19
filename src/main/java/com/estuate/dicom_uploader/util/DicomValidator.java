package com.estuate.dicom_uploader.util;

public class DicomValidator {

    /**
     * Basic check for DICOM file by inspecting the magic "DICM" tag at byte offset 128.
     * @param data byte array of the file
     * @return true if valid DICOM, false otherwise
     */
    public static boolean isValidDicom(byte[] data) {
        if (data == null || data.length < 132) return false;
        return data[128] == 'D' && data[129] == 'I' && data[130] == 'C' && data[131] == 'M';
    }
}
