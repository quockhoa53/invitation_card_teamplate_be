package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.ImportSchemaKeysRequest;
import com.invitation.backend.dto.request.TemplateSchemaKeyRequest;
import com.invitation.backend.dto.response.ImportSchemaKeysResponse;
import com.invitation.backend.dto.response.TemplateSchemaKeyResponse;
import com.invitation.backend.service.TemplateSchemaKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/schema-keys")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchemaKeyController {

    private final TemplateSchemaKeyService schemaKeyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateSchemaKeyResponse>>> getAllSchemaKeys() {
        List<TemplateSchemaKeyResponse> keys = schemaKeyService.getAllSchemaKeysForAdmin();
        return ResponseEntity.ok(ApiResponse.ok(keys));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateSchemaKeyResponse>> createSchemaKey(
            @Valid @RequestBody TemplateSchemaKeyRequest request) {
        TemplateSchemaKeyResponse response = schemaKeyService.createSchemaKey(request);
        return ResponseEntity.ok(ApiResponse.ok("Thêm Schema Key thành công", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateSchemaKeyResponse>> updateSchemaKey(
            @PathVariable UUID id,
            @Valid @RequestBody TemplateSchemaKeyRequest request) {
        TemplateSchemaKeyResponse response = schemaKeyService.updateSchemaKey(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật Schema Key thành công", response));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<TemplateSchemaKeyResponse>> toggleStatus(@PathVariable UUID id) {
        TemplateSchemaKeyResponse response = schemaKeyService.toggleSchemaKeyStatus(id);
        return ResponseEntity.ok(ApiResponse.ok("Đổi trạng thái Schema Key thành công", response));
    }

    @PostMapping("/seed-defaults")
    public ResponseEntity<ApiResponse<List<TemplateSchemaKeyResponse>>> seedDefaults() {
        List<TemplateSchemaKeyResponse> keys = schemaKeyService.seedDefaultSchemaKeys();
        return ResponseEntity.ok(ApiResponse.ok("Khôi phục danh sách Schema Keys chuẩn thành công", keys));
    }

    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ImportSchemaKeysResponse>> importSchemaKeys(
            @Valid @RequestBody ImportSchemaKeysRequest request) {
        ImportSchemaKeysResponse response = schemaKeyService.importSchemaKeys(request);
        String message = String.format("Import thành công: %d tạo mới, %d cập nhật, %d bỏ qua",
                response.getCreatedCount(), response.getUpdatedCount(), response.getSkippedCount());
        return ResponseEntity.ok(ApiResponse.ok(message, response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSchemaKey(@PathVariable UUID id) {
        schemaKeyService.deleteSchemaKey(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa Schema Key thành công", null));
    }
}
