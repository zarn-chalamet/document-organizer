package com.projects.document_organizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryDto {
    private long totalCategories;
    private long totalDocuments;
    private long expiringSoonCount;   // Expiring in next 30 days
    private long expiredCount;
    private long noExpiryDateCount;
}