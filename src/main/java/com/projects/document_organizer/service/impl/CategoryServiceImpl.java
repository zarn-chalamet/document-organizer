package com.projects.document_organizer.service.impl;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.projects.document_organizer.dto.CategoryRequestDto;
import com.projects.document_organizer.dto.CategoryResponseDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import com.projects.document_organizer.model.Category;
import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.CategoryRepository;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.CategoryService;
import com.projects.document_organizer.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    @Transactional
    public CategoryResponseDto createCategory(CategoryRequestDto dto, String email) {
        User user = getUser(email);
        String accessToken = userService.getValidAccessToken(email);

        try {
            Drive drive = buildDrive(accessToken);

            // Ensure user's root folder exists
            String rootFolderId = getOrCreateRootFolder(drive, user);

            // Create the category folder inside root
            String folderName = dto.getName();
            String categoryFolderId = createDriveFolder(drive, folderName, rootFolderId);

            Category category = Category.builder()
                    .name(dto.getName())
                    .type(dto.getType())
                    .customType(dto.getCustomType())
                    .driveFolderId(categoryFolderId)
                    .createdAt(LocalDateTime.now())
                    .user(user)
                    .build();

            Category saved = categoryRepository.save(category);
            return toDto(saved, false);

        } catch (Exception e) {
            log.error("Error creating category", e);
            throw new RuntimeException("Failed to create category: " + e.getMessage());
        }
    }

    @Override
    public List<CategoryResponseDto> getAllCategories(String email) {
        User user = getUser(email);
        return categoryRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(c -> toDto(c, false))
                .toList();
    }

    @Override
    public CategoryResponseDto getCategoryById(Long id, String email) {
        User user = getUser(email);
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return toDto(category, true);
    }

    @Override
    @Transactional
    public CategoryResponseDto updateCategory(Long id, CategoryRequestDto dto, String email) {
        User user = getUser(email);
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String accessToken = userService.getValidAccessToken(email);

        try {
            // If name changed, rename Drive folder too
            if (!category.getName().equals(dto.getName())) {
                Drive drive = buildDrive(accessToken);
                File updated = new File();
                updated.setName(dto.getName());
                drive.files().update(category.getDriveFolderId(), updated).execute();
            }

            category.setName(dto.getName());
            category.setType(dto.getType());
            category.setCustomType(dto.getCustomType());

            Category saved = categoryRepository.save(category);
            return toDto(saved, false);

        } catch (Exception e) {
            log.error("Error updating category", e);
            throw new RuntimeException("Failed to update category: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, String email) {
        User user = getUser(email);
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        String accessToken = userService.getValidAccessToken(email);

        try {
            Drive drive = buildDrive(accessToken);
            // Deleting the folder in Drive also deletes all files inside
            drive.files().delete(category.getDriveFolderId()).execute();
        } catch (Exception e) {
            log.warn("Failed to delete Drive folder (may already be gone): {}", e.getMessage());
            // Continue anyway — DB cleanup still needed
        }

        // JPA cascade deletes all documents inside
        categoryRepository.delete(category);
    }

    // ============ Helpers ============

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Drive buildDrive(String accessToken) {
        HttpRequestInitializer requestInitializer = request ->
                request.getHeaders().setAuthorization("Bearer " + accessToken);

        return new Drive.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName("Digital Document Organizer")
                .build();
    }

    private String getOrCreateRootFolder(Drive drive, User user) throws Exception {
        String folderName = "Digital Document Organizer - " + user.getName();
        String query = String.format(
                "mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false",
                folderName);

        List<File> folders = drive.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()
                .getFiles();

        if (folders != null && !folders.isEmpty()) {
            return folders.get(0).getId();
        }

        return createDriveFolder(drive, folderName, null);
    }

    private String createDriveFolder(Drive drive, String name, String parentId) throws Exception {
        File folderMetadata = new File();
        folderMetadata.setName(name);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");
        if (parentId != null) {
            folderMetadata.setParents(List.of(parentId));
        }

        File folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }

    private CategoryResponseDto toDto(Category c, boolean includeDocuments) {
        List<DocumentResponseDto> docs = null;
        if (includeDocuments && c.getDocuments() != null) {
            docs = c.getDocuments().stream()
                    .map(this::documentToDto)
                    .toList();
        }

        return CategoryResponseDto.builder()
                .id(c.getId())
                .name(c.getName())
                .type(c.getType())
                .customType(c.getCustomType())
                .createdAt(c.getCreatedAt())
                .documentCount(c.getDocuments() != null ? c.getDocuments().size() : 0)
                .documents(docs)
                .build();
    }

    private DocumentResponseDto documentToDto(Document d) {
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