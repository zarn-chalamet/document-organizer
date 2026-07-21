package com.projects.document_organizer.controller;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // Upload a file to a specific category
    @PostMapping("/categories/{categoryId}/upload")
    public ResponseEntity<DocumentResponseDto> uploadFileToCategory(
            @PathVariable Long categoryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description) {

        String email = getCurrentUserEmail();

        DocumentRequestDto requestDto = DocumentRequestDto.builder()
                .title(title)
                .description(description)
                .build();

        DocumentResponseDto uploaded = documentService.uploadFileToCategory(
                file, requestDto, categoryId, email);

        return ResponseEntity.ok(uploaded);
    }

    // Get a single document
    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDto> getDocumentById(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(documentService.getDocumentById(id, email));
    }

    // Delete a single document
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        documentService.deleteDocument(id, email);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserEmail() {
        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return user.getEmail();
    }
}