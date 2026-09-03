package com.invitation.backend.service;

import com.invitation.backend.dto.request.TemplateCategoryRequest;
import com.invitation.backend.dto.response.TemplateCategoryResponse;
import com.invitation.backend.entity.TemplateCategory;

import java.util.List;
import java.util.UUID;

public interface TemplateCategoryService {
    List<TemplateCategoryResponse> getActiveCategories();
    List<TemplateCategoryResponse> getAllCategoriesForAdmin();
    TemplateCategoryResponse getCategoryById(UUID id);
    TemplateCategoryResponse createCategory(TemplateCategoryRequest request);
    TemplateCategoryResponse updateCategory(UUID id, TemplateCategoryRequest request);
    TemplateCategoryResponse toggleCategoryStatus(UUID id);
    void deleteCategory(UUID id);
    TemplateCategoryResponse mapToResponse(TemplateCategory category);
}
