package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.TemplateCategoryRequest;
import com.invitation.backend.dto.response.TemplateCategoryResponse;
import com.invitation.backend.service.TemplateCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final TemplateCategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateCategoryResponse>>> getAllCategories() {
        List<TemplateCategoryResponse> categories = categoryService.getAllCategoriesForAdmin();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateCategoryResponse>> createCategory(
            @Valid @RequestBody TemplateCategoryRequest request) {
        TemplateCategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.ok("Táº¡o loáº¡i template thÃ nh cÃ´ng", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateCategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateCategoryRequest request) {
        TemplateCategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cáº­p nháº­t loáº¡i template thÃ nh cÃ´ng", response));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<TemplateCategoryResponse>> toggleStatus(@PathVariable UUID id) {
        TemplateCategoryResponse response = categoryService.toggleCategoryStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Äá»•i tráº¡ng thÃ¡i loáº¡i template thÃ nh cÃ´ng", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("XÃ³a loáº¡i template thÃ nh cÃ´ng", null));
    }
}

