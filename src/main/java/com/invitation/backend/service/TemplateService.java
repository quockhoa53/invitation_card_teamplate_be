package com.invitation.backend.service;

import com.invitation.backend.dto.request.TemplateRequest;
import com.invitation.backend.dto.response.TemplateResponse;
import com.invitation.backend.entity.Template;

import java.util.List;
import java.util.UUID;

public interface TemplateService {
    List<TemplateResponse> getActiveTemplates();
    List<TemplateResponse> getTemplatesByCategory(String category);
    List<TemplateResponse> getAllTemplatesForAdmin();
    TemplateResponse getTemplateBySlug(String slug);
    TemplateResponse getTemplateById(UUID id);
    TemplateResponse createTemplate(TemplateRequest request);
    TemplateResponse updateTemplate(UUID id, TemplateRequest request);
    TemplateResponse publishTemplate(UUID id);
    void deleteTemplate(UUID id);
    TemplateResponse mapToResponse(Template template);
    List<UUID> getPurchasedTemplateIds(com.invitation.backend.entity.User user);
    boolean purchaseTemplate(com.invitation.backend.entity.User user, UUID templateId);
}
