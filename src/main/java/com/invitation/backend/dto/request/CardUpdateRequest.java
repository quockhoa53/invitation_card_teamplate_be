package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CardUpdateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String slug;
    private String passcode; // Empty string or null means don't change or clear
    private Boolean clearPasscode;
    private String customData;
    private Boolean isPublished;
}

