package com.projects.document_organizer.service;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.model.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    void uploadFileToGoogleDrive(MultipartFile file, DocumentRequestDto requestDto);

    List<Document> getAllDocumentsByUser(String accessToken);
}
