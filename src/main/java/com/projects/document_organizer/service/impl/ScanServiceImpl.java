package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.ScanStatus;
import com.projects.document_organizer.repository.DocumentRepository;
import com.projects.document_organizer.service.EmbeddingService;
import com.projects.document_organizer.service.OcrService;
import com.projects.document_organizer.service.ScanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScanServiceImpl implements ScanService {

    private final DocumentRepository documentRepository;
    private final OcrService ocrService;
    private final EmbeddingService embeddingService;

    @Override
    @Async
    @Transactional
    public void scanDocumentAsync(Long documentId, byte[] fileBytes, String fileType) {
        log.info("Starting async scan for document {}", documentId);

        // Mark as PROCESSING
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        document.setScanStatus(ScanStatus.PROCESSING);
        documentRepository.save(document);

        try {
            // Extract text based on file type
            String fullText = extractText(fileBytes, fileType);

            if (fullText == null || fullText.isBlank()) {
                log.warn("No text extracted for document {}", documentId);
                document.setScanStatus(ScanStatus.FAILED);
                document.setScannedAt(LocalDateTime.now());
                documentRepository.save(document);
                return;
            }

            // Run extraction logic
            LocalDate extractedExpiry = extractExpiryDate(fullText);
            String detectedType = detectDocumentType(fullText);

            // Save results
            document.setExtractedText(fullText);
            document.setExtractedExpiryDate(extractedExpiry);
            document.setDetectedDocumentType(detectedType);
            document.setScanStatus(ScanStatus.DONE);
            document.setScannedAt(LocalDateTime.now());

            // Auto-fill expiryDate if user hasn't set one yet
            if (document.getExpiryDate() == null && extractedExpiry != null) {
                document.setExpiryDate(extractedExpiry);
                log.info("Auto-filled expiry date {} for document {}", extractedExpiry, documentId);
            }

            documentRepository.save(document);
            log.info("Scan complete for document {} — type: {}, expiry: {}",
                    documentId, detectedType, extractedExpiry);

            // Store embeddings for semantic search
            try {
                embeddingService.embedAndStoreDocument(documentId, fullText);
            } catch (Exception e) {
                log.error("Embedding failed for document {} — chat search won't work: {}",
                        documentId, e.getMessage());
                // Don't fail the whole scan if embedding fails
            }

        } catch (Exception e) {
            log.error("Scan failed for document {}: {}", documentId, e.getMessage());
            document.setScanStatus(ScanStatus.FAILED);
            document.setScannedAt(LocalDateTime.now());
            documentRepository.save(document);
        }
    }

    // ── Text Extraction ─────────────────────────────────────────────────────

    private String extractText(byte[] fileBytes, String fileType) {
        if (fileType == null) return "";

        if (fileType.equals("application/pdf")) {
            List<String> pages = ocrService.extractTextFromPdf(fileBytes);
            return String.join("\n--- PAGE BREAK ---\n", pages);
        }

        if (fileType.startsWith("image/")) {
            return ocrService.extractTextFromImage(fileBytes);
        }

        log.warn("Unsupported file type for OCR: {}", fileType);
        return "";
    }

    // ── Expiry Date Extraction ──────────────────────────────────────────────

    private LocalDate extractExpiryDate(String text) {
        // Pattern 1: DD MMM YYYY  (e.g. 15 JAN 2027, 03 MAR 2025)
        Pattern p1 = Pattern.compile(
                "\\b(\\d{1,2})\\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Z]*\\s+(\\d{4})\\b",
                Pattern.CASE_INSENSITIVE);

        // Pattern 2: DD/MM/YYYY or DD-MM-YYYY
        Pattern p2 = Pattern.compile(
                "\\b(\\d{2})[/\\-](\\d{2})[/\\-](\\d{4})\\b");

        // Pattern 3: YYYY-MM-DD (ISO format)
        Pattern p3 = Pattern.compile(
                "\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");

        // Pattern 4: Month DD, YYYY (e.g. January 15, 2027)
        Pattern p4 = Pattern.compile(
                "\\b(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{1,2}),?\\s+(\\d{4})\\b",
                Pattern.CASE_INSENSITIVE);

        // Look for expiry keywords near dates
        String[] expiryKeywords = {
                "expir", "expiry", "expires", "expiration",
                "valid until", "valid thru", "date of expiry",
                "validity", "valid to"
        };

        String lowerText = text.toLowerCase();

        // Try each pattern — prefer dates near expiry keywords
        for (Pattern pattern : List.of(p1, p2, p3, p4)) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                LocalDate candidate = parseDate(matcher, pattern);
                if (candidate == null) continue;

                // Check if expiry keyword is near this date (within 100 chars)
                int start = Math.max(0, matcher.start() - 100);
                int end = Math.min(lowerText.length(), matcher.end() + 100);
                String context = lowerText.substring(start, end);

                for (String keyword : expiryKeywords) {
                    if (context.contains(keyword)) {
                        // Only return future dates as expiry
                        if (candidate.isAfter(LocalDate.now().minusYears(1))) {
                            return candidate;
                        }
                    }
                }
            }
        }

        // Fallback — return any future date found
        for (Pattern pattern : List.of(p1, p2, p3, p4)) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                LocalDate candidate = parseDate(matcher, pattern);
                if (candidate != null && candidate.isAfter(LocalDate.now())) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private LocalDate parseDate(Matcher matcher, Pattern pattern) {
        try {
            String patternStr = pattern.pattern();

            // Pattern 1: DD MMM YYYY
            if (patternStr.contains("JAN|FEB")) {
                int day = Integer.parseInt(matcher.group(1));
                String monthStr = matcher.group(2).toUpperCase().substring(0, 3);
                int year = Integer.parseInt(matcher.group(3));
                int month = List.of("JAN","FEB","MAR","APR","MAY","JUN",
                        "JUL","AUG","SEP","OCT","NOV","DEC").indexOf(monthStr) + 1;
                return LocalDate.of(year, month, day);
            }

            // Pattern 2: DD/MM/YYYY
            if (patternStr.contains("[/\\\\-]")) {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                if (month > 12 || day > 31) return null;
                return LocalDate.of(year, month, day);
            }

            // Pattern 3: YYYY-MM-DD
            if (patternStr.startsWith("\\b(\\d{4})")) {
                int year = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int day = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            }

            // Pattern 4: Month DD, YYYY
            if (patternStr.contains("January")) {
                String monthName = matcher.group(1);
                int day = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM d yyyy");
                return LocalDate.parse(monthName + " " + day + " " + year, fmt);
            }

        } catch (Exception e) {
            log.debug("Failed to parse date from match: {}", e.getMessage());
        }
        return null;
    }

    // ── Document Type Detection ─────────────────────────────────────────────

    private String detectDocumentType(String text) {
        String lower = text.toLowerCase();

        if (lower.contains("passport")) return "Passport";
        if (lower.contains("visa")) return "Visa";
        if (lower.contains("work permit")) return "Work Permit";
        if (lower.contains("driving licence") || lower.contains("driver's license")
                || lower.contains("driving license")) return "Driver License";
        if (lower.contains("national id") || lower.contains("identity card")
                || lower.contains("identification")) return "ID Card";
        if (lower.contains("insurance")) return "Insurance";
        if (lower.contains("certificate")) return "Certificate";
        if (lower.contains("residence permit") || lower.contains("permanent resident"))
            return "Residence Permit";

        return "Unknown";
    }
}