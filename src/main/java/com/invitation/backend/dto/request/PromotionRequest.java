package com.invitation.backend.dto.request;

import com.invitation.backend.entity.Promotion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PromotionRequest {

    @NotBlank(message = "Mã khuyến mãi không được để trống")
    private String code;

    private String description;

    @NotNull(message = "Loại khuyến mãi không được để trống")
    private Promotion.DiscountType discountType; // PERCENTAGE, FIXED_AMOUNT

    @NotNull(message = "Giá trị khuyến mãi không được để trống")
    private Long discountValue;

    private Long minOrderAmount = 0L;

    private Long maxDiscountAmount;

    private Integer maxUsage = 100;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean isActive = true;
}
