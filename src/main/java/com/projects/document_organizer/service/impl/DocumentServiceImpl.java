package com.projects.document_organizer.service.impl;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.client.http.InputStreamContent;
import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.DocumentRepository;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.DocumentService;
import com.projects.document_organizer.service.UserService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public DocumentResponseDto getDocumentById(Long id, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Security check — make sure user owns this document
        if (!doc.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Access denied");
        }

        return DocumentResponseDto.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .expiryDate(doc.getExpiryDate())
                .driveFileLink(doc.getDriveFileLink())
                .fileType(doc.getFileType())
                .build();
    }

    @Override
    public void uploadFileToGoogleDrive(MultipartFile file, 
                                        DocumentRequestDto requestDto, 
                                        String email) {
        try {
            // Get valid token (handles refresh automatically)
            String accessToken = userService.getValidAccessToken(email);

            HttpRequestInitializer requestInitializer = request ->
                    request.getHeaders().setAuthorization("Bearer " + accessToken);

            Drive drive = new Drive.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName("Digital Document Organizer")
                    .build();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get or create user's personal folder
            String folderId = getOrCreateUserFolder(drive, user);

            // Prepare metadata
            File fileMetadata = new File();
            fileMetadata.setName(file.getOriginalFilename());
            fileMetadata.setParents(List.of(folderId));

            InputStreamContent mediaContent = new InputStreamContent(
                file.getContentType(),
                file.getInputStream()
            );

            // Upload file to Drive
            File uploadedFile = drive.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();

            // Save to database
            Document document = Document.builder()
                    .title(requestDto.getTitle())
                    .description(requestDto.getDescription())
                    .expiryDate(requestDto.getExpiryDate())
                    .driveFileId(uploadedFile.getId())
                    .driveFileLink(uploadedFile.getWebViewLink())
                    .fileType(file.getContentType())
                    .user(user)
                    .build();

            documentRepository.save(document);

        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to Google Drive", e);
        }
    }

    @Override
    public List<DocumentResponseDto> getAllDocumentsByUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return user.getDocuments().stream()
                .map(doc -> DocumentResponseDto.builder()
                        .id(doc.getId())
                        .title(doc.getTitle())
                        .description(doc.getDescription())
                        .expiryDate(doc.getExpiryDate())
                        .driveFileLink(doc.getDriveFileLink())
                        .fileType(doc.getFileType())
                        .build())
                .toList();
    }

    private String getOrCreateUserFolder(Drive drive, User user) throws Exception {
        String folderName = "Digital Document Organizer - " + user.getName();

        String query = String.format(
                "mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false",
                folderName
        );

        List<File> folders = drive.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()
                .getFiles();

        if (folders != null && !folders.isEmpty()) {
            return folders.get(0).getId();
        }

        File folderMetadata = new File();
        folderMetadata.setName(folderName);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = drive.files().create(folderMetadata)
                .setFields("id")
                .execute();

        return folder.getId();
    }
}
