package com.projects.document_organizer.controller;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.DocumentRepository;
import com.projects.document_organizer.service.ScanService;
import com.projects.document_organizer.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/api/documents")
@RequiredArgsConstructor
public class ScanController {

    private final DocumentRepository documentRepository;
    private final ScanService scanService;
    private final UserService userService;

    // Manual rescan — for when auto-scan fails
    @PostMapping("/{id}/scan")
    public ResponseEntity<?> rescanDocument(@PathVariable Long id) {
        User user = getCurrentUser();

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Ownership check
        if (!document.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        // Download file bytes from Google Drive
        try {
            String accessToken = userService.getValidAccessToken(user.getEmail());
            byte[] fileBytes = downloadFromDrive(document.getDriveFileId(), accessToken);
            scanService.scanDocumentAsync(id, fileBytes, document.getFileType());
            return ResponseEntity.ok(Map.of("message", "Scan started", "documentId", id));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to start scan: " + e.getMessage()));
        }
    }

    private byte[] downloadFromDrive(String fileId, String accessToken) throws Exception {
        com.google.api.client.http.HttpRequestInitializer initializer =
                req -> req.getHeaders().setAuthorization("Bearer " + accessToken);

        com.google.api.services.drive.Drive drive = new com.google.api.services.drive.Drive.Builder(
                new com.google.api.client.http.javanet.NetHttpTransport(),
                com.google.api.client.json.jackson2.JacksonFactory.getDefaultInstance(),
                initializer)
                .setApplicationName("Digital Document Organizer")
                .build();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        drive.files().get(fileId).executeMediaAndDownloadTo(baos);
        return baos.toByteArray();
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}