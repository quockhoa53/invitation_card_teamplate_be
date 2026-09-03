package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.response.TemplateResponse;
import com.invitation.backend.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.invitation.backend.entity.User;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.security.UserDetailsImpl;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getActiveTemplates()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplatesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getTemplatesByCategory(category)));
    }

    @GetMapping("/my-purchases")
    public ResponseEntity<ApiResponse<List<UUID>>> getMyPurchases(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }
        User user = userRepository.findById(userDetails.getId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(templateService.getPurchasedTemplateIds(user)));
    }

    @PostMapping("/{id}/purchase")
    public ResponseEntity<ApiResponse<Boolean>> purchaseTemplate(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để mua mẫu thiệp");
        }
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean success = templateService.purchaseTemplate(user, id);
        return ResponseEntity.ok(ApiResponse.ok("Mua và mở khóa mẫu thiệp thành công!", success));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplateBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getTemplateBySlug(slug)));
    }
}

