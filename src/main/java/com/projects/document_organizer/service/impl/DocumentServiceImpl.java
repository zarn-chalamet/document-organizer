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
    public void uploadFileToGoogleDrive(MultipartFile file, DocumentRequestDto requestDto) {
        try {
            // Initialize Drive
            HttpRequestInitializer requestInitializer = request ->
                    request.getHeaders().setAuthorization("Bearer " + requestDto.getAccessToken());

            Drive drive = new Drive.Builder(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    requestInitializer)
                    .setApplicationName("Digital Document Organizer")
                    .build();

            // Find the user
            User user = userRepository.findByGoogleAccessToken(requestDto.getAccessToken())
                    .orElseThrow(() -> new RuntimeException("User not found for this access token"));

            // Get or create user folder
            String folderId = getOrCreateUserFolder(drive, user);

            // Prepare metadata and file content
            File fileMetadata = new File();
            fileMetadata.setName(file.getOriginalFilename());
            fileMetadata.setParents(List.of(folderId)); // <== PUT FILE IN USER’S FOLDER

            java.io.File tempFile = convertToFile(file);
            FileContent mediaContent = new FileContent(file.getContentType(), tempFile);

            // Upload file
            File uploadedFile = drive.files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();

            // Save document record
            Document document = Document.builder()
                    .title(requestDto.getTitle())
                    .description(requestDto.getDescription())
                    .expiryDate(requestDto.getExpiryDate())
                    .driveFileId(uploadedFile.getId())
                    .driveFileLink(uploadedFile.getWebViewLink())
                    .fileType(file.getContentType())
                    .user(user)
                    .build();

            user.getDocuments().add(document);
            userRepository.save(user);

            // Clean up
            if (!tempFile.delete()) tempFile.deleteOnExit();

        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to Google Drive", e);
        }
    }


    @Override
    public List<Document> getAllDocumentsByUser(String accessToken) {

        //find user by access token
        User user = userRepository.findByGoogleAccessToken(accessToken)
                .orElseThrow(() -> new RuntimeException("User not found for this access token"));

        return user.getDocuments();
    }

    private String getOrCreateUserFolder(Drive drive, User user) throws Exception {
        String folderName = "Digital Document Organizer - " + user.getName();

        // Check if folder already exists
        String query = String.format("mimeType='application/vnd.google-apps.folder' and name='%s' and trashed=false", folderName);
        List<File> folders = drive.files().list()
                .setQ(query)
                .setFields("files(id, name)")
                .execute()
                .getFiles();

        if (folders != null && !folders.isEmpty()) {
            // Folder already exists — return its ID
            return folders.get(0).getId();
        }

        // Otherwise create a new folder
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
