package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWishRequest {
    @NotBlank(message = "Sender name is required")
    @Size(max = 100, message = "Name is too long")
    private String senderName;

    @NotBlank(message = "Message is required")
    @Size(max = 1000, message = "Message is too long")
    private String message;

    private String emotionIcon;
}

