package com.invitation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {
    private UUID id;
    private String title;
    private String slug;
    private String description;
    private String category;
    private String thumbnailUrl;
    private String previewUrl;
    private Boolean isFree;
    private Long price;
    private String defaultConfig;
    private String schemaRules;
    private String templateType;
    private String customHtml;
    private String customCss;
    private String customJs;
    private Boolean isPublished;
    private Boolean isActive;
    private Long usageCount;
    private LocalDateTime createdAt;
}

