package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Verify2FARequest {
    @NotBlank(message = "Temporary token is required")
    private String tempToken;

    private String code; // 6-digit Google Authenticator code

    private String backupCode; // Optional backup recovery code
}

