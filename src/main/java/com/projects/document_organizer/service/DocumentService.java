package com.projects.document_organizer.service;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    void uploadFileToGoogleDrive(User user, MultipartFile file, DocumentRequestDto requestDto);

    List<Document> getAllDocumentsByUser(User user);
}
