package com.projects.document_organizer.dto;

import com.projects.document_organizer.model.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequestDto {
    private String name;
    private CategoryType type;
    private String customType; // Only used when type == CUSTOM
}