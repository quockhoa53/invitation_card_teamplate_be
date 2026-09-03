package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.PromotionRequest;
import com.invitation.backend.dto.response.PromotionDto;
import com.invitation.backend.dto.response.ValidatePromotionResponse;
import com.invitation.backend.service.PromotionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;

    // Public / User validate promotion code
    @GetMapping("/promotions/validate")
    public ResponseEntity<ApiResponse<ValidatePromotionResponse>> validatePromotion(
            @RequestParam String code,
            @RequestParam(defaultValue = "0") Long amount) {
        ValidatePromotionResponse response = promotionService.validatePromotion(code, amount);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // Admin APIs
    @GetMapping("/admin/promotions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<PromotionDto>>> getAdminPromotions(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PromotionDto> result = promotionService.getAdminPromotions(search, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/admin/promotions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PromotionDto>> getPromotionById(@PathVariable UUID id) {
        PromotionDto result = promotionService.getPromotionById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/admin/promotions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PromotionDto>> createPromotion(@Valid @RequestBody PromotionRequest request) {
        PromotionDto result = promotionService.createPromotion(request);
        return ResponseEntity.ok(ApiResponse.ok("Tạo mã khuyến mãi thành công", result));
    }

    @PutMapping("/admin/promotions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PromotionDto>> updatePromotion(
            @PathVariable UUID id,
            @Valid @RequestBody PromotionRequest request) {
        PromotionDto result = promotionService.updatePromotion(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật mã khuyến mãi thành công", result));
    }

    @DeleteMapping("/admin/promotions/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> deletePromotion(@PathVariable UUID id) {
        promotionService.deletePromotion(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa mã khuyến mãi", true));
    }

    @PatchMapping("/admin/promotions/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PromotionDto>> togglePromotion(@PathVariable UUID id) {
        PromotionDto result = promotionService.togglePromotionActive(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã thay đổi trạng thái mã khuyến mãi", result));
    }
}
