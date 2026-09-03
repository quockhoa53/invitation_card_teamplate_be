package com.invitation.backend.controller;

import com.invitation.backend.dto.response.AdminStatsResponse;
import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.TemplateRequest;
import com.invitation.backend.dto.response.TemplateResponse;
import com.invitation.backend.dto.response.UserDto;
import com.invitation.backend.entity.Role;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.service.AdminService;
import com.invitation.backend.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final TemplateService templateService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAdminStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllUsers()));
    }

    @PatchMapping("/users/{userId}/toggle-status")
    public ResponseEntity<ApiResponse<UserDto>> toggleUserStatus(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok("User status updated", adminService.toggleUserStatus(userId)));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> body) {
        Role role = Role.valueOf(body.get("role"));
        return ResponseEntity.ok(ApiResponse.ok("User role updated", adminService.updateUserRole(userId, role)));
    }

    // Admin Templates Endpoints (Drafts & Published)
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getAllAdminTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getAllTemplatesForAdmin()));
    }

    @PostMapping("/templates")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(@Valid @RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.ok(ApiResponse.ok("Báº£n nhÃ¡p template Ä‘Ã£ Ä‘Æ°á»£c táº¡o thÃ nh cÃ´ng", response));
    }

    @PutMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateRequest request) {
        TemplateResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cáº­p nháº­t thÃ´ng tin template thÃ nh cÃ´ng", response));
    }

    @PatchMapping("/templates/{id}/publish")
    public ResponseEntity<ApiResponse<TemplateResponse>> publishTemplate(@PathVariable UUID id) {
        TemplateResponse response = templateService.publishTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok("Template Ä‘Ã£ Ä‘Æ°á»£c Xuáº¥t Báº£n cÃ´ng khai thÃ nh cÃ´ng!", response));
    }

    @DeleteMapping("/templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok("Template Ä‘Ã£ Ä‘Æ°á»£c xÃ³a thÃ nh cÃ´ng", null));
    }
}

