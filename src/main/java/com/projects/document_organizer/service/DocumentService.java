package com.projects.document_organizer.service;

import com.projects.document_organizer.dto.DocumentRequestDto;
import com.projects.document_organizer.dto.DocumentResponseDto;
import com.projects.document_organizer.dto.DocumentUpdateDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentResponseDto uploadFileToCategory(MultipartFile file,
                                              DocumentRequestDto requestDto,
                                              Long categoryId,
                                              String email);

    List<DocumentResponseDto> uploadMultipleFilesToCategory(MultipartFile[] files,
                                                             Long categoryId,
                                                             String email);

    DocumentResponseDto getDocumentById(Long id, String email);

    DocumentResponseDto updateDocument(Long id, DocumentUpdateDto dto, String email);

    DocumentResponseDto moveDocument(Long id, Long targetCategoryId, String email);

    void deleteDocument(Long id, String email);
}