package com.projects.document_organizer.service;

import java.util.List;

public interface OcrService {

    // Extract text from image bytes (JPG, PNG)
    String extractTextFromImage(byte[] imageBytes);

    // Extract text from PDF bytes — returns text per page
    List<String> extractTextFromPdf(byte[] pdfBytes);
}