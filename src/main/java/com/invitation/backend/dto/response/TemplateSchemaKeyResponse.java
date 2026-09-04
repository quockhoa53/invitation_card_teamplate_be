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
public class TemplateSchemaKeyResponse {
    private UUID id;
    private String keyName;
    private String label;
    private String fieldType;
    private String sectionName;
    private String placeholder;
    private String description;
    private String defaultValue;
    private Boolean isRequired;
    private Integer displayOrder;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
