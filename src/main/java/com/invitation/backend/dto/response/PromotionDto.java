package com.invitation.backend.dto.response;

import com.invitation.backend.entity.Promotion;
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
public class PromotionDto {
    private UUID id;
    private String code;
    private String description;
    private String discountType;
    private Long discountValue;
    private Long minOrderAmount;
    private Long maxDiscountAmount;
    private Integer maxUsage;
    private Integer usedCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static PromotionDto fromEntity(Promotion p) {
        return PromotionDto.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .discountType(p.getDiscountType() != null ? p.getDiscountType().name() : "FIXED_AMOUNT")
                .discountValue(p.getDiscountValue())
                .minOrderAmount(p.getMinOrderAmount())
                .maxDiscountAmount(p.getMaxDiscountAmount())
                .maxUsage(p.getMaxUsage())
                .usedCount(p.getUsedCount())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .isActive(p.getIsActive())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
