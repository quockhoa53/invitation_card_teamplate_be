package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.CardCreateRequest;
import com.invitation.backend.dto.request.CardUpdateRequest;
import com.invitation.backend.dto.request.CreateWishRequest;
import com.invitation.backend.dto.response.CardResponse;
import com.invitation.backend.dto.response.PublicCardResponse;
import com.invitation.backend.dto.response.WishResponse;
import com.invitation.backend.entity.Card;
import com.invitation.backend.entity.CardWish;
import com.invitation.backend.entity.Template;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import com.invitation.backend.repository.CardRepository;
import com.invitation.backend.repository.CardWishRepository;
import com.invitation.backend.repository.TemplateRepository;
import com.invitation.backend.repository.TransactionRepository;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.service.CardService;
import com.invitation.backend.service.QrCodeGeneratorService;
import com.invitation.backend.service.TemplateService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final TemplateRepository templateRepository;
    private final CardWishRepository cardWishRepository;
    private final TemplateService templateService;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final com.invitation.backend.repository.UserTemplatePurchaseRepository userTemplatePurchaseRepository;

    @Value("${app.client.url:http://localhost:5173}")
    private String clientUrl;

    @Override
    @Transactional
    public CardResponse createCard(User user, CardCreateRequest request) {
        Template template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        // Deduct credits if paid template and not yet purchased
        if (!Boolean.TRUE.equals(template.getIsFree()) && template.getPrice() != null && template.getPrice() > 0) {
            User freshUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            boolean alreadyPurchased = userTemplatePurchaseRepository.existsByUserAndTemplate(freshUser, template);
            if (!alreadyPurchased) {
                long price = template.getPrice();
                long totalCredits = freshUser.getCreditsBalance() != null ? freshUser.getCreditsBalance() : 0L;

                if (totalCredits < price) {
                    throw new IllegalArgumentException(String.format(
                            "INSUFFICIENT_CREDITS: Bạn chưa mua mẫu thiệp này và số dư ví không đủ (Hiện có: %,d đ, Giá mẫu: %,d đ). Vui lòng nạp thêm tiền!",
                            totalCredits, price
                    ));
                }

                // Deduct priority: bonusBalance first, then realBalance
                long bonus = freshUser.getBonusBalance() != null ? freshUser.getBonusBalance() : 0L;
                long bonusDeduct = Math.min(bonus, price);
                long realDeduct = price - bonusDeduct;

                userRepository.atomicDeductForPurchase(freshUser.getId(), realDeduct, bonusDeduct);

                // Save template purchase record
                com.invitation.backend.entity.UserTemplatePurchase purchase = com.invitation.backend.entity.UserTemplatePurchase.builder()
                        .user(freshUser)
                        .template(template)
                        .pricePaid(price)
                        .build();
                userTemplatePurchaseRepository.save(purchase);

                // Record purchase transaction
                Transaction purchaseTx = Transaction.builder()
                        .user(freshUser)
                        .orderCode("BUY" + System.currentTimeMillis())
                        .paymentMethod("WALLET")
                        .amount(price)
                        .type("CARD_PURCHASE")
                        .status(Transaction.Status.SUCCESS)
                        .gatewayPayload(String.format("{\"template\": \"%s\", \"realDeduct\": %d, \"bonusDeduct\": %d}",
                                template.getTitle(), realDeduct, bonusDeduct))
                        .completedAt(LocalDateTime.now())
                        .build();
                transactionRepository.save(purchaseTx);
            }
        }

        String slug = request.getSlug();
        if (slug == null || slug.trim().isEmpty() || cardRepository.existsBySlug(slug.trim())) {
            slug = generateUniqueSlug(template.getSlug());
        } else {
            slug = slug.trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        }

        String passcodeHash = null;
        if (request.getPasscode() != null && !request.getPasscode().trim().isEmpty()) {
            passcodeHash = passwordEncoder.encode(request.getPasscode().trim());
        }

        String publicUrl = clientUrl + "/c/" + slug;
        String qrCodeBase64 = qrCodeGeneratorService.generateQrCodeBase64(publicUrl, 300, 300);

        String customData = request.getCustomData();
        if (customData == null || customData.trim().isEmpty()) {
            customData = template.getDefaultConfig();
        }

        Card card = Card.builder()
                .user(user)
                .template(template)
                .title(request.getTitle())
                .slug(slug)
                .passcodeHash(passcodeHash)
                .customData(customData)
                .qrCodeBase64(qrCodeBase64)
                .isPublished(true)
                .viewCount(0L)
                .build();

        card = cardRepository.save(card);

        // Increment template usage count
        template.setUsageCount(template.getUsageCount() + 1);
        templateRepository.save(template);

        return mapToResponse(card);
    }

    @Override
    public List<CardResponse> getUserCards(User user) {
        return cardRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CardResponse getCardById(User user, UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (!card.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You do not have access to this card");
        }
        return mapToResponse(card);
    }

    @Override
    @Transactional
    public CardResponse updateCard(User user, UUID id, CardUpdateRequest request) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (!card.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You do not have access to this card");
        }

        card.setTitle(request.getTitle());

        if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
            String newSlug = request.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");
            if (!newSlug.equals(card.getSlug())) {
                if (cardRepository.existsBySlug(newSlug)) {
                    throw new IllegalArgumentException("Slug is already taken");
                }
                card.setSlug(newSlug);
                String publicUrl = clientUrl + "/c/" + newSlug;
                card.setQrCodeBase64(qrCodeGeneratorService.generateQrCodeBase64(publicUrl, 300, 300));
            }
        }

        if (Boolean.TRUE.equals(request.getClearPasscode())) {
            card.setPasscodeHash(null);
        } else if (request.getPasscode() != null && !request.getPasscode().trim().isEmpty()) {
            card.setPasscodeHash(passwordEncoder.encode(request.getPasscode().trim()));
        }

        if (request.getCustomData() != null) {
            card.setCustomData(request.getCustomData());
        }

        if (request.getIsPublished() != null) {
            card.setIsPublished(request.getIsPublished());
        }

        return mapToResponse(cardRepository.save(card));
    }

    @Override
    @Transactional
    public void deleteCard(User user, UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        if (!card.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You do not have access to this card");
        }
        cardRepository.delete(card);
    }

    @Override
    @Transactional
    public PublicCardResponse getPublicCardBySlug(String slug) {
        Card card = cardRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with slug: " + slug));

        if (!Boolean.TRUE.equals(card.getIsPublished())) {
            throw new IllegalStateException("This card is currently private or unpublished");
        }

        // Increment view count
        card.setViewCount(card.getViewCount() + 1);
        cardRepository.save(card);

        boolean isProtected = card.getPasscodeHash() != null && !card.getPasscodeHash().isEmpty();
        List<WishResponse> wishes = isProtected
                ? java.util.Collections.emptyList()
                : cardWishRepository.findByCardOrderByCreatedAtDesc(card).stream()
                        .map(this::mapWishToResponse)
                        .collect(Collectors.toList());

        return PublicCardResponse.builder()
                .id(card.getId())
                .template(templateService.mapToResponse(card.getTemplate()))
                .title(isProtected ? "Thiệp Mời Riêng Tư" : card.getTitle())
                .slug(card.getSlug())
                .isProtected(isProtected)
                .customData(isProtected ? null : card.getCustomData())
                .isPublished(card.getIsPublished())
                .viewCount(card.getViewCount())
                .wishes(wishes)
                .createdAt(card.getCreatedAt())
                .build();
    }

    @Override
    public PublicCardResponse verifyPasscodeAndGetCard(String slug, String passcode) {
        Card card = cardRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Card not found"));

        if (card.getPasscodeHash() != null && !passwordEncoder.matches(passcode, card.getPasscodeHash())) {
            throw new IllegalArgumentException("Incorrect passcode for this card");
        }

        List<WishResponse> wishes = cardWishRepository.findByCardOrderByCreatedAtDesc(card).stream()
                .map(this::mapWishToResponse)
                .collect(Collectors.toList());

        return PublicCardResponse.builder()
                .id(card.getId())
                .template(templateService.mapToResponse(card.getTemplate()))
                .title(card.getTitle())
                .slug(card.getSlug())
                .isProtected(false)
                .customData(card.getCustomData())
                .isPublished(card.getIsPublished())
                .viewCount(card.getViewCount())
                .wishes(wishes)
                .createdAt(card.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public WishResponse addWish(String slug, CreateWishRequest request) {
        Card card = cardRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with slug: " + slug));

        if (!Boolean.TRUE.equals(card.getIsPublished())) {
            throw new IllegalStateException("Cannot add wish to an unpublished card");
        }

        // XSS sanitization
        String sanitizedSender = HtmlUtils.htmlEscape(request.getSenderName().trim());
        String sanitizedMessage = HtmlUtils.htmlEscape(request.getMessage().trim());

        CardWish wish = CardWish.builder()
                .card(card)
                .senderName(sanitizedSender)
                .message(sanitizedMessage)
                .build();

        wish = cardWishRepository.save(wish);
        return mapWishToResponse(wish);
    }

    @Override
    public List<WishResponse> getCardWishes(String slug) {
        Card card = cardRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with slug: " + slug));
        return cardWishRepository.findByCardOrderByCreatedAtDesc(card).stream()
                .map(this::mapWishToResponse)
                .collect(Collectors.toList());
    }

    private String generateUniqueSlug(String base) {
        String baseSlug = (base != null && !base.isEmpty())
                ? base.toLowerCase().replaceAll("[^a-z0-9-]", "-")
                : "card";
        String randomSuffix = RandomStringUtils.randomAlphanumeric(6).toLowerCase();
        return baseSlug + "-" + randomSuffix;
    }

    @Override
    public CardResponse mapToResponse(Card card) {
        return CardResponse.builder()
                .id(card.getId())
                .template(templateService.mapToResponse(card.getTemplate()))
                .title(card.getTitle())
                .slug(card.getSlug())
                .hasPasscode(card.getPasscodeHash() != null && !card.getPasscodeHash().isEmpty())
                .customData(card.getCustomData())
                .qrCodeBase64(card.getQrCodeBase64())
                .publicUrl(clientUrl + "/c/" + card.getSlug())
                .isPublished(card.getIsPublished())
                .viewCount(card.getViewCount())
                .wishesCount(cardWishRepository.countByCard(card))
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    @Override
    public WishResponse mapWishToResponse(CardWish wish) {
        return WishResponse.builder()
                .id(wish.getId())
                .senderName(wish.getSenderName())
                .message(wish.getMessage())
                .createdAt(wish.getCreatedAt())
                .build();
    }
}
