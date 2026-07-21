package com.projects.document_organizer.service.impl;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import com.projects.document_organizer.model.Category;
import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.CategoryRepository;
import com.projects.document_organizer.repository.DocumentRepository;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.DocumentService;
import com.projects.document_organizer.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    @Transactional
    public DocumentResponseDto uploadFileToCategory(MultipartFile file,
                                                     DocumentRequestDto requestDto,
                                                     Long categoryId,
                                                     String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Category category = categoryRepository.findByIdAndUser(categoryId, user)
                    .orElseThrow(() -> new RuntimeException("Category not found"));

            String accessToken = userService.getValidAccessToken(email);

            HttpRequestInitializer requestInitializer = request ->
                    request.getHeaders().setAuthorization("Bearer " + accessToken);

            Drive drive = new Drive.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName("Digital Document Organizer")
                    .build();

            // Upload directly into the category's folder
            File fileMetadata = new File();
            fileMetadata.setName(file.getOriginalFilename());
            fileMetadata.setParents(List.of(category.getDriveFolderId()));

            InputStreamContent mediaContent = new InputStreamContent(
                    file.getContentType(),
                    file.getInputStream()
            );

            File uploadedFile = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();

            Document document = Document.builder()
                    .title(requestDto.getTitle())
                    .description(requestDto.getDescription())
                    .driveFileId(uploadedFile.getId())
                    .driveFileLink(uploadedFile.getWebViewLink())
                    .fileType(file.getContentType())
                    .user(user)
                    .category(category)
                    .build();

            Document saved = documentRepository.save(document);
            return toDto(saved);

        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new RuntimeException("Error uploading file: " + e.getMessage());
        }
    }

    @Override
    public DocumentResponseDto getDocumentById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        return toDto(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        if (!doc.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        // Delete from Google Drive first
        try {
            String accessToken = userService.getValidAccessToken(email);
            HttpRequestInitializer requestInitializer = request ->
                    request.getHeaders().setAuthorization("Bearer " + accessToken);

            Drive drive = new Drive.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName("Digital Document Organizer")
                    .build();

            drive.files().delete(doc.getDriveFileId()).execute();
        } catch (Exception e) {
            log.warn("Failed to delete file from Drive: {}", e.getMessage());
            // Continue anyway — remove from DB
        }

        documentRepository.delete(doc);
    }

    private DocumentResponseDto toDto(Document d) {
        return DocumentResponseDto.builder()
                .id(d.getId())
                .title(d.getTitle())
                .description(d.getDescription())
                .expiryDate(d.getExpiryDate())
                .driveFileLink(d.getDriveFileLink())
                .fileType(d.getFileType())
                .categoryId(d.getCategory() != null ? d.getCategory().getId() : null)
                .build();
    }
}