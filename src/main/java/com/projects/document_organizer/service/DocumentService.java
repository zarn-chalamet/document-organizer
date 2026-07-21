package com.projects.document_organizer.service;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentResponseDto getDocumentById(Long id, String email);

    void uploadFileToGoogleDrive(MultipartFile file, 
                                  DocumentRequestDto requestDto, 
                                  String email);

    List<DocumentResponseDto> getAllDocumentsByUser(String email);
}