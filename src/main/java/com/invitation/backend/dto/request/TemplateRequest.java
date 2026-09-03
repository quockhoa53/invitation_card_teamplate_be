package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TemplateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Slug is required")
    private String slug;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    private String thumbnailUrl;
    private String previewUrl;

    @NotNull(message = "isFree must be specified")
    private Boolean isFree;

    private Long price;
    private String defaultConfig;
    private String schemaRules;
    private String templateType; // BUILT_IN or CUSTOM_CODE
    private String customHtml;
    private String customCss;
    private String customJs;
    private Boolean isPublished; // Draft or Published
    private Boolean isActive;
}

