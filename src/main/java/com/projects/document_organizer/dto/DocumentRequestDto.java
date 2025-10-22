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
public class DocumentRequestDto {

    private String accessToken;
    private String title;
    private String description;
    private LocalDate expiryDate;
}
