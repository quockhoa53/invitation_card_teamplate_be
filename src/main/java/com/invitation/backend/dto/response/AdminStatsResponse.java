package com.invitation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long totalCards;
    private long totalTemplates;
    private long totalRevenue;
    private long totalTransactions;
    private long activeTemplatesCount;
    private long publishedCardsCount;
    private List<TransactionDto> recentTransactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDto {
        private String orderCode;
        private String userEmail;
        private String userName;
        private Long amount;
        private String paymentMethod;
        private String status;
        private String createdAt;
    }
}

