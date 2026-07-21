package com.projects.document_organizer.controller;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import com.projects.document_organizer.dto.DocumentResponseDto;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/v1/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/test")
    public ResponseEntity<?> testing() {
        System.out.println("testing phase");
        return ResponseEntity.ok("Testing done.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponseDto> getDocumentById(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        DocumentResponseDto doc = documentService.getDocumentById(id, email);
        return ResponseEntity.ok(doc);
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFileToDrive(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("expiryDate") LocalDate expiryDate) {

        String email = getCurrentUserEmail();

        DocumentRequestDto requestDto = DocumentRequestDto.builder()
                .title(title)
                .description(description)
                .expiryDate(expiryDate)
                .build();

        documentService.uploadFileToGoogleDrive(file, requestDto, email);
        return ResponseEntity.ok("File uploaded successfully");
    }

    @GetMapping()
    public ResponseEntity<List<DocumentResponseDto>> getDocumentsByUser() {
        String email = getCurrentUserEmail();
        List<DocumentResponseDto> documents = documentService.getAllDocumentsByUser(email);
        return ResponseEntity.ok(documents);
    }

    // Gets email from JWT token (already validated by JwtAuthenticationFilter)
    private String getCurrentUserEmail() {
        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return user.getEmail();
    }
}