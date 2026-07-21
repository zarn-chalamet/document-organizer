package com.projects.document_organizer.controller;

import com.projects.document_organizer.dto.DocumentMoveDto;
import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import com.projects.document_organizer.dto.DocumentUpdateDto;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/v1/api")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    // Upload single file
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

        return ResponseEntity.ok(
                documentService.uploadFileToCategory(file, requestDto, categoryId, email));
    }

    // Bulk upload multiple files
    @PostMapping("/categories/{categoryId}/upload-multiple")
    public ResponseEntity<List<DocumentResponseDto>> uploadMultipleFiles(
            @PathVariable Long categoryId,
            @RequestParam("files") MultipartFile[] files) {

        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                documentService.uploadMultipleFilesToCategory(files, categoryId, email));
    }

    // Get single document
    @GetMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDto> getDocumentById(@PathVariable Long id) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(documentService.getDocumentById(id, email));
    }

    // Update document (title, description, expiry date)
    @PatchMapping("/documents/{id}")
    public ResponseEntity<DocumentResponseDto> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentUpdateDto dto) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(documentService.updateDocument(id, dto, email));
    }

    // Move document to another category
    @PatchMapping("/documents/{id}/move")
    public ResponseEntity<DocumentResponseDto> moveDocument(
            @PathVariable Long id,
            @RequestBody DocumentMoveDto dto) {
        String email = getCurrentUserEmail();
        return ResponseEntity.ok(
                documentService.moveDocument(id, dto.getTargetCategoryId(), email));
    }

    // Delete document
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