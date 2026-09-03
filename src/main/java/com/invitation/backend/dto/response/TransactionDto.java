package com.invitation.backend.dto.response;

import com.invitation.backend.entity.Transaction;
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
public class TransactionDto {
    private UUID id;
    private String orderCode;
    private String paymentMethod;
    private Long amount;
    private String status;
    private String gatewayPayload;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    
    // User metadata
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String userAvatarUrl;

    public static TransactionDto fromEntity(Transaction t) {
        TransactionDto.TransactionDtoBuilder builder = TransactionDto.builder()
                .id(t.getId())
                .orderCode(t.getOrderCode())
                .paymentMethod(t.getPaymentMethod())
                .amount(t.getAmount())
                .status(t.getStatus() != null ? t.getStatus().name() : "PENDING")
                .gatewayPayload(t.getGatewayPayload())
                .completedAt(t.getCompletedAt())
                .createdAt(t.getCreatedAt());

        if (t.getUser() != null) {
            builder.userId(t.getUser().getId())
                   .userEmail(t.getUser().getEmail())
                   .userFullName(t.getUser().getFullName())
                   .userAvatarUrl(t.getUser().getAvatarUrl());
        }

        return builder.build();
    }
}

