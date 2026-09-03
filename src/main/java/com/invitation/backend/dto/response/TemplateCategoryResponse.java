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
public class TemplateCategoryResponse {
    private UUID id;
    private String code;
    private String name;
    private String emoji;
    private String description;
    private Integer displayOrder;
    private Boolean isActive;
    private Long templateCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

