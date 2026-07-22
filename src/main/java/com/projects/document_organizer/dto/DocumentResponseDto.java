package com.projects.document_organizer.dto;

import com.projects.document_organizer.model.ScanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentResponseDto {
    private Long id;
    private String title;
    private String description;
    private LocalDate expiryDate;
    private String driveFileLink;
    private String fileType;
    private Long categoryId;

    // AI Scan Fields
    private ScanStatus scanStatus;
    private LocalDate extractedExpiryDate;
    private String detectedDocumentType;
}