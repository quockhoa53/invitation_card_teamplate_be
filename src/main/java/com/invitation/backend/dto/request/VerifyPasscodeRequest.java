package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPasscodeRequest {
    @NotBlank(message = "Passcode is required")
    private String passcode;
}

