package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.response.TemplateResponse;
import com.invitation.backend.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplates() {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getActiveTemplates()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplatesByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getTemplatesByCategory(category)));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplateBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(templateService.getTemplateBySlug(slug)));
    }
}

