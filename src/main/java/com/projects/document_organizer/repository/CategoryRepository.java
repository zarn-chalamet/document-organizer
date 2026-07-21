package com.projects.document_organizer.repository;

import com.projects.document_organizer.model.Category;
import com.projects.document_organizer.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUserOrderByCreatedAtDesc(User user);

    Optional<Category> findByIdAndUser(Long id, User user);

    long countByUser(User user);
}