package com.projects.document_organizer.service.impl;

import com.projects.document_organizer.dto.DashboardSummaryDto;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.repository.CategoryRepository;
import com.projects.document_organizer.repository.DocumentRepository;
import com.projects.document_organizer.repository.UserRepository;
import com.projects.document_organizer.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    @Override
    public DashboardSummaryDto getSummary(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);

        return DashboardSummaryDto.builder()
                .totalCategories(categoryRepository.countByUser(user))
                .totalDocuments(documentRepository.countByUser(user))
                .expiringSoonCount(documentRepository.countByUserAndExpiryDateBetween(user, today, in30Days))
                .expiredCount(documentRepository.countByUserAndExpiryDateBefore(user, today))
                .noExpiryDateCount(documentRepository.countByUserAndExpiryDateIsNull(user))
                .build();
    }
}