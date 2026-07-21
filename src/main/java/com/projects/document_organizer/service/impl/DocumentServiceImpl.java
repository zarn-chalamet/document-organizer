package com.projects.document_organizer.service.impl;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import com.projects.document_organizer.dto.DocumentUpdateDto;
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

import java.util.ArrayList;
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
            User user = getUser(email);
            Category category = getCategory(categoryId, user);
            Drive drive = buildDrive(email);

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

            return toDto(documentRepository.save(document));

        } catch (Exception e) {
            log.error("Upload failed", e);
            throw new RuntimeException("Error uploading file: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public List<DocumentResponseDto> uploadMultipleFilesToCategory(MultipartFile[] files,
                                                                    Long categoryId,
                                                                    String email) {
        List<DocumentResponseDto> results = new ArrayList<>();
        for (MultipartFile file : files) {
            // Use file's original name as title
            String title = file.getOriginalFilename();
            if (title != null && title.contains(".")) {
                title = title.substring(0, title.lastIndexOf("."));
            }

            DocumentRequestDto dto = DocumentRequestDto.builder()
                    .title(title)
                    .description(null)
                    .build();

            try {
                results.add(uploadFileToCategory(file, dto, categoryId, email));
            } catch (Exception e) {
                log.error("Failed to upload one of bulk files: {}", file.getOriginalFilename(), e);
                // Continue with other files
            }
        }
        return results;
    }

    @Override
    public DocumentResponseDto getDocumentById(Long id, String email) {
        User user = getUser(email);
        Document doc = getDocumentOwnedBy(id, user);
        return toDto(doc);
    }

    @Override
    @Transactional
    public DocumentResponseDto updateDocument(Long id, DocumentUpdateDto dto, String email) {
        User user = getUser(email);
        Document doc = getDocumentOwnedBy(id, user);

        boolean titleChanged = dto.getTitle() != null && !dto.getTitle().equals(doc.getTitle());

        if (dto.getTitle() != null) doc.setTitle(dto.getTitle());
        if (dto.getDescription() != null) doc.setDescription(dto.getDescription());
        if (dto.getExpiryDate() != null) doc.setExpiryDate(dto.getExpiryDate());

        // Rename file in Drive if title changed
        if (titleChanged) {
            try {
                Drive drive = buildDrive(email);
                File update = new File();
                update.setName(dto.getTitle());
                drive.files().update(doc.getDriveFileId(), update).execute();
            } catch (Exception e) {
                log.warn("Failed to rename Drive file: {}", e.getMessage());
                // Continue — DB update still valid
            }
        }

        return toDto(documentRepository.save(doc));
    }

    @Override
    @Transactional
    public DocumentResponseDto moveDocument(Long id, Long targetCategoryId, String email) {
        User user = getUser(email);
        Document doc = getDocumentOwnedBy(id, user);
        Category target = getCategory(targetCategoryId, user);

        // Skip if already in target
        if (doc.getCategory() != null && doc.getCategory().getId().equals(targetCategoryId)) {
            return toDto(doc);
        }

        // Move file in Drive
        try {
            Drive drive = buildDrive(email);

            String previousParents = null;
            if (doc.getCategory() != null) {
                previousParents = doc.getCategory().getDriveFolderId();
            }

            drive.files().update(doc.getDriveFileId(), null)
                    .setAddParents(target.getDriveFolderId())
                    .setRemoveParents(previousParents)
                    .setFields("id, parents")
                    .execute();

        } catch (Exception e) {
            log.error("Failed to move file in Drive", e);
            throw new RuntimeException("Failed to move file: " + e.getMessage());
        }

        doc.setCategory(target);
        return toDto(documentRepository.save(doc));
    }

    @Override
    @Transactional
    public void deleteDocument(Long id, String email) {
        User user = getUser(email);
        Document doc = getDocumentOwnedBy(id, user);

        try {
            Drive drive = buildDrive(email);
            drive.files().delete(doc.getDriveFileId()).execute();
        } catch (Exception e) {
            log.warn("Failed to delete file from Drive: {}", e.getMessage());
        }

        documentRepository.delete(doc);
    }

    // ============ Helpers ============

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Category getCategory(Long id, User user) {
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Category not found"));
    }

    private Document getDocumentOwnedBy(Long id, User user) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (!doc.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        return doc;
    }

    private Drive buildDrive(String email) {
        String accessToken = userService.getValidAccessToken(email);
        HttpRequestInitializer requestInitializer = request ->
                request.getHeaders().setAuthorization("Bearer " + accessToken);

        return new Drive.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Digital Document Organizer")
                .build();
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