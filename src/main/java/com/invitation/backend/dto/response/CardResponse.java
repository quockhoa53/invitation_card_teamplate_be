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
public class CardResponse {
    private UUID id;
    private TemplateResponse template;
    private String title;
    private String slug;
    private boolean hasPasscode;
    private String customData;
    private String qrCodeBase64;
    private String publicUrl;
    private Boolean isPublished;
    private Long viewCount;
    private Long wishesCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

