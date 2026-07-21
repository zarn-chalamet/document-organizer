package com.projects.document_organizer.controller;

import com.projects.document_organizer.dto.CategoryRequestDto;
import com.projects.document_organizer.dto.CategoryResponseDto;
import com.projects.document_organizer.model.User;
import com.projects.document_organizer.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponseDto> createCategory(
            @RequestBody CategoryRequestDto dto) {
        return ResponseEntity.ok(categoryService.createCategory(dto, getCurrentUserEmail()));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDto>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories(getCurrentUserEmail()));
    }

    // Now supports ?search=xxx and ?filter=expiring|expired|no-date
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getCategoryById(
            @PathVariable Long id,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter) {
        return ResponseEntity.ok(
                categoryService.getCategoryById(id, getCurrentUserEmail(), search, filter));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryRequestDto dto) {
        return ResponseEntity.ok(
                categoryService.updateCategory(id, dto, getCurrentUserEmail()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id, getCurrentUserEmail());
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserEmail() {
        User user = (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
        return user.getEmail();
    }
}