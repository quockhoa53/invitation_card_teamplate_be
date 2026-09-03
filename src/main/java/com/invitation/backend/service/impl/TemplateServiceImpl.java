package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.TemplateRequest;
import com.invitation.backend.dto.response.TemplateResponse;
import com.invitation.backend.entity.Template;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import com.invitation.backend.entity.UserTemplatePurchase;
import com.invitation.backend.repository.TemplateRepository;
import com.invitation.backend.repository.TransactionRepository;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.repository.UserTemplatePurchaseRepository;
import com.invitation.backend.service.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateServiceImpl implements TemplateService {

    private final TemplateRepository templateRepository;
    private final UserTemplatePurchaseRepository userTemplatePurchaseRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public List<TemplateResponse> getActiveTemplates() {
        return templateRepository.findByIsActiveTrueAndIsPublishedTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateResponse> getTemplatesByCategory(String category) {
        return templateRepository.findByIsActiveTrueAndIsPublishedTrueAndCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateResponse> getAllTemplatesForAdmin() {
        return templateRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TemplateResponse getTemplateBySlug(String slug) {
        Template template = templateRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with slug: " + slug));
        return mapToResponse(template);
    }

    @Override
    public TemplateResponse getTemplateById(UUID id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found with ID: " + id));
        return mapToResponse(template);
    }

    @Override
    @Transactional
    public TemplateResponse createTemplate(TemplateRequest request) {
        if (templateRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Template with slug already exists");
        }

        Boolean isPublished = Boolean.TRUE.equals(request.getIsPublished());

        Template template = Template.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .category(request.getCategory())
                .thumbnailUrl(request.getThumbnailUrl())
                .previewUrl(request.getPreviewUrl())
                .isFree(request.getIsFree() != null ? request.getIsFree() : true)
                .price(request.getPrice() != null ? request.getPrice() : 0L)
                .defaultConfig(request.getDefaultConfig())
                .schemaRules(request.getSchemaRules())
                .templateType(request.getTemplateType() != null ? request.getTemplateType() : "BUILT_IN")
                .customHtml(request.getCustomHtml())
                .customCss(request.getCustomCss())
                .customJs(request.getCustomJs())
                .isPublished(isPublished)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .usageCount(0L)
                .build();

        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(UUID id, TemplateRequest request) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (!template.getSlug().equals(request.getSlug()) && templateRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug already in use");
        }

        template.setTitle(request.getTitle());
        template.setSlug(request.getSlug());
        template.setDescription(request.getDescription());
        template.setCategory(request.getCategory());
        template.setThumbnailUrl(request.getThumbnailUrl());
        template.setPreviewUrl(request.getPreviewUrl());
        if (request.getIsFree() != null) template.setIsFree(request.getIsFree());
        if (request.getPrice() != null) template.setPrice(request.getPrice());
        if (request.getDefaultConfig() != null) template.setDefaultConfig(request.getDefaultConfig());
        if (request.getSchemaRules() != null) template.setSchemaRules(request.getSchemaRules());
        if (request.getTemplateType() != null) template.setTemplateType(request.getTemplateType());
        if (request.getCustomHtml() != null) template.setCustomHtml(request.getCustomHtml());
        if (request.getCustomCss() != null) template.setCustomCss(request.getCustomCss());
        if (request.getCustomJs() != null) template.setCustomJs(request.getCustomJs());
        if (request.getIsPublished() != null) template.setIsPublished(request.getIsPublished());
        if (request.getIsActive() != null) template.setIsActive(request.getIsActive());

        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse publishTemplate(UUID id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        template.setIsPublished(true);
        return mapToResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        Template template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        if (Boolean.TRUE.equals(template.getIsPublished())) {
            throw new IllegalStateException("Template này ĐÃ XUẤT BẢN và đang phục vụ người dùng. Để bảo toàn dữ liệu thiệp đã tạo, template này KHÔNG ĐƯỢC PHÉP XÓA mà chỉ có thể chỉnh sửa thông tin hoặc cập nhật code!");
        }

        templateRepository.delete(template);
    }

    @Override
    public TemplateResponse mapToResponse(Template template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .title(template.getTitle())
                .slug(template.getSlug())
                .description(template.getDescription())
                .category(template.getCategory())
                .thumbnailUrl(template.getThumbnailUrl())
                .previewUrl(template.getPreviewUrl())
                .isFree(template.getIsFree())
                .price(template.getPrice())
                .defaultConfig(template.getDefaultConfig())
                .schemaRules(template.getSchemaRules())
                .templateType(template.getTemplateType())
                .customHtml(template.getCustomHtml())
                .customCss(template.getCustomCss())
                .customJs(template.getCustomJs())
                .isPublished(template.getIsPublished() != null ? template.getIsPublished() : false)
                .isActive(template.getIsActive())
                .usageCount(template.getUsageCount())
                .createdAt(template.getCreatedAt())
                .build();
    }

    @Override
    public List<UUID> getPurchasedTemplateIds(User user) {
        if (user == null) return List.of();
        return userTemplatePurchaseRepository.findPurchasedTemplateIdsByUserId(user.getId());
    }

    @Override
    @Transactional
    public boolean purchaseTemplate(User user, UUID templateId) {
        Template template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mẫu thiệp này"));

        // Free templates don't require purchase
        if (Boolean.TRUE.equals(template.getIsFree()) || template.getPrice() == null || template.getPrice() <= 0) {
            return true;
        }

        // Check if already purchased
        if (userTemplatePurchaseRepository.existsByUserAndTemplate(user, template)) {
            return true;
        }

        User freshUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thông tin người dùng"));

        long totalCredits = freshUser.getCreditsBalance() != null ? freshUser.getCreditsBalance() : 0L;
        long price = template.getPrice();

        if (totalCredits < price) {
            throw new IllegalArgumentException(String.format(
                    "INSUFFICIENT_CREDITS: Số dư ví không đủ để mua mẫu này (Hiện có: %,d đ, Cần: %,d đ). Vui lòng nạp thêm tiền!",
                    totalCredits, price
            ));
        }

        // Deduct priority: bonusBalance first, then realBalance
        long bonus = freshUser.getBonusBalance() != null ? freshUser.getBonusBalance() : 0L;
        long bonusDeduct = Math.min(bonus, price);
        long realDeduct = price - bonusDeduct;

        int rows = userRepository.atomicDeductForPurchase(freshUser.getId(), realDeduct, bonusDeduct);
        if (rows == 0) {
            throw new IllegalStateException("Số dư ví đã thay đổi. Vui lòng thử lại!");
        }

        // Save purchase record
        UserTemplatePurchase purchase = UserTemplatePurchase.builder()
                .user(freshUser)
                .template(template)
                .pricePaid(price)
                .build();
        userTemplatePurchaseRepository.save(purchase);

        // Record audited Transaction
        String orderCode = "BUY" + System.currentTimeMillis();
        Transaction tx = Transaction.builder()
                .user(freshUser)
                .orderCode(orderCode)
                .paymentMethod("WALLET")
                .amount(price)
                .type("CARD_PURCHASE")
                .status(Transaction.Status.SUCCESS)
                .gatewayPayload(String.format("{\"templateId\": \"%s\", \"templateTitle\": \"%s\", \"realDeduct\": %d, \"bonusDeduct\": %d}",
                        template.getId(), template.getTitle(), realDeduct, bonusDeduct))
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(tx);

        log.info("User {} successfully purchased template {} for {} VND (Real: {}, Bonus: {})",
                freshUser.getEmail(), template.getTitle(), price, realDeduct, bonusDeduct);

        return true;
    }
}
