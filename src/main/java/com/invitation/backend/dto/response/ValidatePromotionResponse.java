package com.invitation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidatePromotionResponse {
    private boolean valid;
    private String message;
    private String code;
    private String discountType;
    private Long discountValue;
    private Long originalAmount;
    private Long discountAmount;
    private Long finalAmount;
}
