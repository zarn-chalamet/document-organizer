package com.projects.document_organizer.service.impl;

import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.DocumentRepository;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.DocumentService;
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

    @Override
    @Transactional
    public void uploadFileToGoogleDrive(User user, MultipartFile file, DocumentRequestDto requestDto) {
        try {
            System.out.println("Upload file phase started...");
            System.out.println("User received: " + user);

            // Initialize Google Drive API
            HttpRequestInitializer requestInitializer = request ->
                    request.getHeaders().setAuthorization("Bearer " + user.getGoogleAccessToken());

            Drive drive = new Drive.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName("Digital Document Organizer")
                    .build();

            // Get or create user's personal folder
            String folderId = getOrCreateUserFolder(drive, user);

            // Prepare metadata
            File fileMetadata = new File();
            fileMetadata.setName(file.getOriginalFilename());
            fileMetadata.setParents(List.of(folderId));

            java.io.File tempFile = convertToFile(file);
            FileContent mediaContent = new FileContent(file.getContentType(), tempFile);

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

            // Clean up temp file
            if (!tempFile.delete()) tempFile.deleteOnExit();

        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to Google Drive", e);
        }
    }

    @Override
    public List<Document> getAllDocumentsByUser(User user) {
        System.out.println("get all documents - user " + user);

        List<Document> documents = documentRepository.findAllByUser(user);
        System.out.println("documents"+ documents);

        return documents;
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

    private java.io.File convertToFile(MultipartFile multipartFile) {
        try {
            java.io.File convFile = java.io.File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(convFile);
            return convFile;
        } catch (Exception e) {
            throw new RuntimeException("Error converting multipart file to file", e);
        }
    }
}
