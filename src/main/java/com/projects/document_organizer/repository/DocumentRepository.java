package com.projects.document_organizer.repository;

import com.projects.document_organizer.model.Document;
import com.projects.document_organizer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document,Long> {

    List<Document> findByExpiryDateBetween(LocalDate today, LocalDate threeDaysFromNow);

    List<Document> findByExpiryDateBefore(LocalDate today);

    List<Document> findAllByUser(User user);

}
