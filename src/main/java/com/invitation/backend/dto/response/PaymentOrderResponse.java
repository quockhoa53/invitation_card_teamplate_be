package com.invitation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentOrderResponse {
    private String orderCode;
    private Long amount;
    private String paymentMethod;
    private String vietQrUrl;
    private String qrCodeBase64;
    private String bankName;
    private String bankAccountNo;
    private String accountHolder;
    private String transferContent;
    private String status;
    private Long bonusAmount;
    private Long actualAmount;
    private Long missingAmount;
    private LocalDateTime createdAt;
}

