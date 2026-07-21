package com.projects.document_organizer.dto;

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
}