package com.invitation.backend.service;

import com.invitation.backend.dto.request.PromotionRequest;
import com.invitation.backend.dto.response.PromotionDto;
import com.invitation.backend.dto.response.ValidatePromotionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PromotionService {

    ValidatePromotionResponse validatePromotion(String code, Long orderAmount);

    void recordPromotionUsage(String code);

    Page<PromotionDto> getAdminPromotions(String search, Boolean isActive, Pageable pageable);

    PromotionDto getPromotionById(UUID id);

    PromotionDto createPromotion(PromotionRequest request);

    PromotionDto updatePromotion(UUID id, PromotionRequest request);

    void deletePromotion(UUID id);

    PromotionDto togglePromotionActive(UUID id);
}
