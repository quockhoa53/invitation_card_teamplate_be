package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.TemplateCategoryRequest;
import com.invitation.backend.dto.response.TemplateCategoryResponse;
import com.invitation.backend.entity.TemplateCategory;
import com.invitation.backend.repository.TemplateCategoryRepository;
import com.invitation.backend.repository.TemplateRepository;
import com.invitation.backend.service.TemplateCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateCategoryServiceImpl implements TemplateCategoryService {

    private final TemplateCategoryRepository categoryRepository;
    private final TemplateRepository templateRepository;

    @Override
    public List<TemplateCategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateCategoryResponse> getAllCategoriesForAdmin() {
        return categoryRepository.findAllByOrderByDisplayOrderAscCreatedAtAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TemplateCategoryResponse getCategoryById(UUID id) {
        TemplateCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại template"));
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public TemplateCategoryResponse createCategory(TemplateCategoryRequest request) {
        String normalizedCode = request.getCode().trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        if (categoryRepository.existsByCode(normalizedCode)) {
            throw new IllegalArgumentException("Mã danh mục '" + normalizedCode + "' đã tồn tại");
        }

        TemplateCategory category = TemplateCategory.builder()
                .code(normalizedCode)
                .name(request.getName().trim())
                .emoji(request.getEmoji() != null && !request.getEmoji().trim().isEmpty() ? request.getEmoji().trim() : "✨")
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public TemplateCategoryResponse updateCategory(UUID id, TemplateCategoryRequest request) {
        TemplateCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại template"));

        String normalizedCode = request.getCode().trim().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        if (categoryRepository.existsByCodeAndIdNot(normalizedCode, id)) {
            throw new IllegalArgumentException("Mã danh mục '" + normalizedCode + "' đã được sử dụng bởi danh mục khác");
        }

        category.setCode(normalizedCode);
        category.setName(request.getName().trim());
        if (request.getEmoji() != null) category.setEmoji(request.getEmoji().trim());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());

        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public TemplateCategoryResponse toggleCategoryStatus(UUID id) {
        TemplateCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại template"));
        category.setIsActive(!Boolean.TRUE.equals(category.getIsActive()));
        return mapToResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(UUID id) {
        TemplateCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại template"));
        categoryRepository.delete(category);
    }

    @Override
    public TemplateCategoryResponse mapToResponse(TemplateCategory category) {
        return TemplateCategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .emoji(category.getEmoji())
                .description(category.getDescription())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
