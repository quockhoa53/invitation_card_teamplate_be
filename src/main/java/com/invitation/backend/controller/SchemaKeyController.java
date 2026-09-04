package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.response.TemplateSchemaKeyResponse;
import com.invitation.backend.service.TemplateSchemaKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schema-keys")
@RequiredArgsConstructor
public class SchemaKeyController {

    private final TemplateSchemaKeyService schemaKeyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateSchemaKeyResponse>>> getActiveSchemaKeys() {
        List<TemplateSchemaKeyResponse> keys = schemaKeyService.getActiveSchemaKeys();
        return ResponseEntity.ok(ApiResponse.ok(keys));
    }
}
