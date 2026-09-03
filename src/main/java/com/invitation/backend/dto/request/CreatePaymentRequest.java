package com.invitation.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreatePaymentRequest {
    private UUID cardId; // Optional card ID if paying for a card
    private UUID templateId; // Optional template ID
    @NotNull(message = "Amount is required")
    private Long amount;
    private String paymentMethod; // VIETQR, PAYOS, MOMO
}

