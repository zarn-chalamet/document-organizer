package com.projects.document_organizer.dto;

import com.projects.document_organizer.model.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDto {
    private Long id;
    private String name;
    private CategoryType type;
    private String customType;
    private LocalDateTime createdAt;
    private int documentCount;
    private List<DocumentResponseDto> documents;
}