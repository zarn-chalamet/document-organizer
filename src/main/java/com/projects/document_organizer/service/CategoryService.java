package com.projects.document_organizer.service;

import com.projects.document_organizer.dto.CategoryRequestDto;
import com.projects.document_organizer.dto.CategoryResponseDto;

import java.util.List;

public interface CategoryService {

    CategoryResponseDto createCategory(CategoryRequestDto dto, String email);

    List<CategoryResponseDto> getAllCategories(String email);

    CategoryResponseDto getCategoryById(Long id, String email, String search, String filter);

    CategoryResponseDto updateCategory(Long id, CategoryRequestDto dto, String email);

    void deleteCategory(Long id, String email);
}