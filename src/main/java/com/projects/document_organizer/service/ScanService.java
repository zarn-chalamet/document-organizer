package com.projects.document_organizer.service;

public interface ScanService {

    // Called after upload — runs in background thread
    void scanDocumentAsync(Long documentId, byte[] fileBytes, String fileType);
}