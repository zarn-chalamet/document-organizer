package com.projects.document_organizer.service;

import com.projects.document_organizer.dto.DashboardSummaryDto;

public interface DashboardService {
    DashboardSummaryDto getSummary(String email);
}