package com.projects.document_organizer.repository;

import com.projects.document_organizer.model.Category;
import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // Existing (used by ExpiryCheckService)
    List<Document> findByExpiryDateBetween(LocalDate start, LocalDate end);
    List<Document> findByExpiryDateBefore(LocalDate date);

    // Count queries for dashboard
    long countByUser(User user);
    long countByUserAndExpiryDateBetween(User user, LocalDate start, LocalDate end);
    long countByUserAndExpiryDateBefore(User user, LocalDate date);
    long countByUserAndExpiryDateIsNull(User user);

    // Search + filter within a category
    List<Document> findByCategoryAndTitleContainingIgnoreCaseOrCategoryAndDescriptionContainingIgnoreCase(
            Category c1, String title, Category c2, String description);

    List<Document> findByCategoryAndExpiryDateBetween(Category category, LocalDate start, LocalDate end);
    List<Document> findByCategoryAndExpiryDateBefore(Category category, LocalDate date);
    List<Document> findByCategoryAndExpiryDateIsNull(Category category);
}