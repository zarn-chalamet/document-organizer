package com.projects.document_organizer.service;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService {

    DocumentResponseDto uploadFileToCategory(MultipartFile file,
                                              DocumentRequestDto requestDto,
                                              Long categoryId,
                                              String email);

    DocumentResponseDto getDocumentById(Long id, String email);

    void deleteDocument(Long id, String email);
}