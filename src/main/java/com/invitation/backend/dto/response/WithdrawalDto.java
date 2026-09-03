package com.invitation.backend.dto.response;

import com.invitation.backend.entity.Withdrawal;
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
public class WithdrawalDto {
    private UUID id;
    private Long amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String status;
    private String adminNote;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    // User metadata
    private UUID userId;
    private String userEmail;
    private String userFullName;
    private String userAvatarUrl;

    public static WithdrawalDto fromEntity(Withdrawal w) {
        WithdrawalDto.WithdrawalDtoBuilder builder = WithdrawalDto.builder()
                .id(w.getId())
                .amount(w.getAmount())
                .bankName(w.getBankName())
                .accountNumber(w.getAccountNumber())
                .accountHolder(w.getAccountHolder())
                .status(w.getStatus() != null ? w.getStatus().name() : "PENDING")
                .adminNote(w.getAdminNote())
                .processedAt(w.getProcessedAt())
                .createdAt(w.getCreatedAt());

        if (w.getUser() != null) {
            builder.userId(w.getUser().getId())
                   .userEmail(w.getUser().getEmail())
                   .userFullName(w.getUser().getFullName())
                   .userAvatarUrl(w.getUser().getAvatarUrl());
        }

        return builder.build();
    }
}
