package com.invitation.backend.service;

import com.invitation.backend.dto.request.TemplateSchemaKeyRequest;
import com.invitation.backend.dto.response.TemplateSchemaKeyResponse;
import com.invitation.backend.entity.TemplateSchemaKey;

import java.util.List;
import java.util.UUID;

public interface TemplateSchemaKeyService {
    List<TemplateSchemaKeyResponse> getActiveSchemaKeys();
    List<TemplateSchemaKeyResponse> getAllSchemaKeysForAdmin();
    TemplateSchemaKeyResponse getSchemaKeyById(UUID id);
    TemplateSchemaKeyResponse createSchemaKey(TemplateSchemaKeyRequest request);
    TemplateSchemaKeyResponse updateSchemaKey(UUID id, TemplateSchemaKeyRequest request);
    TemplateSchemaKeyResponse toggleSchemaKeyStatus(UUID id);
    void deleteSchemaKey(UUID id);
    List<TemplateSchemaKeyResponse> seedDefaultSchemaKeys();
    TemplateSchemaKeyResponse mapToResponse(TemplateSchemaKey entity);
}
