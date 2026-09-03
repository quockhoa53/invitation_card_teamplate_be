package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.PromotionRequest;
import com.invitation.backend.dto.response.PromotionDto;
import com.invitation.backend.dto.response.ValidatePromotionResponse;
import com.invitation.backend.entity.Promotion;
import com.invitation.backend.repository.PromotionRepository;
import com.invitation.backend.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;

    @Override
    public ValidatePromotionResponse validatePromotion(String code, Long orderAmount) {
        if (code == null || code.isBlank()) {
            return ValidatePromotionResponse.builder()
                    .valid(false)
                    .message("Mã khuyến mãi không được để trống")
                    .originalAmount(orderAmount)
                    .discountAmount(0L)
                    .finalAmount(orderAmount)
                    .build();
        }

        Promotion promo = promotionRepository.findByCodeIgnoreCase(code.trim()).orElse(null);
        if (promo == null) {
            return ValidatePromotionResponse.builder()
                    .valid(false)
                    .message("Mã khuyến mãi không tồn tại")
                    .originalAmount(orderAmount)
                    .discountAmount(0L)
                    .finalAmount(orderAmount)
                    .build();
        }

        if (!Boolean.TRUE.equals(promo.getIsActive())) {
            return ValidatePromotionResponse.builder()
                    .valid(false)
                    .message("Mã khuyến mãi này hiện đang bị khóa")
                    .originalAmount(orderAmount)
                    .discountAmount(0L)
                    .finalAmount(orderAmount)
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
            return ValidatePromotionResponse.builder()
                    .valid(false)
                    .message("Mã khuyến mãi này chưa đến ngày áp dụng")
                    .originalAmount(orderAmount)
                    .discountAmount(0L)
                    .finalAmount(orderAmount)
                    .build();
        }

        if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
            return ValidatePromotionResponse.builder()
                    .valid(false)
                    .message("Mã khuyến mãi này đã hết hạn sử dụng")
                    .originalAmount(orderAmount)
                    .discountAmount(0L)
                    .finalAmount(orderAmount)
                    .build();
        }

        if (promo.getMaxUsage() != null && promo.getUsedCount() != null && promo.getUsedCount() >= promo.getMaxUsage()) {
            return ValidatePromotionResponse.builder()
                    .valid(false)
                    .message("Mã khuyến mãi này đã hết lượt sử dụng")
                    .originalAmount(orderAmount)
                    .discountAmount(0L)
                    .finalAmount(orderAmount)
                    .build();
        }

        long safeOrderAmount = orderAmount != null ? orderAmount : 0L;
        if (promo.getMinOrderAmount() != null && safeOrderAmount < promo.getMinOrderAmount()) {
            return ValidatePromotionResponse.builder()
                    .valid(false)
                    .message(String.format("Đơn hàng tối thiểu %,d đ mới được áp dụng mã này",
                            promo.getMinOrderAmount()))
                    .originalAmount(safeOrderAmount)
                    .discountAmount(0L)
                    .finalAmount(safeOrderAmount)
                    .build();
        }

        long discountAmount = 0L;
        if (promo.getDiscountType() == Promotion.DiscountType.PERCENTAGE) {
            discountAmount = (safeOrderAmount * promo.getDiscountValue()) / 100;
            if (promo.getMaxDiscountAmount() != null && discountAmount > promo.getMaxDiscountAmount()) {
                discountAmount = promo.getMaxDiscountAmount();
            }
        } else {
            discountAmount = promo.getDiscountValue();
        }

        discountAmount = Math.min(discountAmount, safeOrderAmount);
        long finalAmount = Math.max(0, safeOrderAmount - discountAmount);

        return ValidatePromotionResponse.builder()
                .valid(true)
                .message("Áp dụng mã giảm giá thành công!")
                .code(promo.getCode())
                .discountType(promo.getDiscountType().name())
                .discountValue(promo.getDiscountValue())
                .originalAmount(safeOrderAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .build();
    }

    @Override
    @Transactional
    public void recordPromotionUsage(String code) {
        if (code == null || code.isBlank()) return;
        promotionRepository.findByCodeIgnoreCase(code.trim()).ifPresent(p -> {
            int current = p.getUsedCount() != null ? p.getUsedCount() : 0;
            p.setUsedCount(current + 1);
            promotionRepository.save(p);
        });
    }

    @Override
    public Page<PromotionDto> getAdminPromotions(String search, Boolean isActive, Pageable pageable) {
        String keyword = (search != null && !search.isBlank()) ? search.trim() : null;
        return promotionRepository.findAllFiltered(keyword, isActive, pageable).map(PromotionDto::fromEntity);
    }

    @Override
    public PromotionDto getPromotionById(UUID id) {
        return promotionRepository.findById(id)
                .map(PromotionDto::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã khuyến mãi"));
    }

    @Override
    @Transactional
    public PromotionDto createPromotion(PromotionRequest request) {
        String code = request.getCode().trim().toUpperCase();
        if (promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã khuyến mãi '" + code + "' đã tồn tại");
        }

        Promotion promo = Promotion.builder()
                .code(code)
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : 0L)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .maxUsage(request.getMaxUsage() != null ? request.getMaxUsage() : 100)
                .usedCount(0)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return PromotionDto.fromEntity(promotionRepository.save(promo));
    }

    @Override
    @Transactional
    public PromotionDto updatePromotion(UUID id, PromotionRequest request) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã khuyến mãi"));

        String code = request.getCode().trim().toUpperCase();
        if (!promo.getCode().equalsIgnoreCase(code) && promotionRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Mã khuyến mãi '" + code + "' đã tồn tại");
        }

        promo.setCode(code);
        promo.setDescription(request.getDescription());
        promo.setDiscountType(request.getDiscountType());
        promo.setDiscountValue(request.getDiscountValue());
        promo.setMinOrderAmount(request.getMinOrderAmount() != null ? request.getMinOrderAmount() : 0L);
        promo.setMaxDiscountAmount(request.getMaxDiscountAmount());
        if (request.getMaxUsage() != null) promo.setMaxUsage(request.getMaxUsage());
        promo.setStartDate(request.getStartDate());
        promo.setEndDate(request.getEndDate());
        if (request.getIsActive() != null) promo.setIsActive(request.getIsActive());

        return PromotionDto.fromEntity(promotionRepository.save(promo));
    }

    @Override
    @Transactional
    public void deletePromotion(UUID id) {
        if (!promotionRepository.existsById(id)) {
            throw new IllegalArgumentException("Không tìm thấy mã khuyến mãi");
        }
        promotionRepository.deleteById(id);
    }

    @Override
    @Transactional
    public PromotionDto togglePromotionActive(UUID id) {
        Promotion promo = promotionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã khuyến mãi"));
        promo.setIsActive(!Boolean.TRUE.equals(promo.getIsActive()));
        return PromotionDto.fromEntity(promotionRepository.save(promo));
    }
}
