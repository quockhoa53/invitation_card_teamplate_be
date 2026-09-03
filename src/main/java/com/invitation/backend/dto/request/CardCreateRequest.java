package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CardCreateRequest {
    @NotNull(message = "Template ID is required")
    private UUID templateId;

    @NotBlank(message = "Title is required")
    private String title;

    private String slug; // Optional custom slug, if empty auto-generate

    private String passcode; // Optional password to protect card

    private String customData; // JSON configuration of user card
}

