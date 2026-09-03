package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Enable2FARequest {
    @NotBlank(message = "Verification code is required")
    private String code;
}

