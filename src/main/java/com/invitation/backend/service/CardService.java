package com.invitation.backend.service;

import com.invitation.backend.dto.request.CardCreateRequest;
import com.invitation.backend.dto.request.CardUpdateRequest;
import com.invitation.backend.dto.request.CreateWishRequest;
import com.invitation.backend.dto.response.CardResponse;
import com.invitation.backend.dto.response.PublicCardResponse;
import com.invitation.backend.dto.response.WishResponse;
import com.invitation.backend.entity.Card;
import com.invitation.backend.entity.CardWish;
import com.invitation.backend.entity.User;

import java.util.List;
import java.util.UUID;

public interface CardService {
    CardResponse createCard(User user, CardCreateRequest request);
    List<CardResponse> getUserCards(User user);
    CardResponse getCardById(User user, UUID id);
    CardResponse updateCard(User user, UUID id, CardUpdateRequest request);
    void deleteCard(User user, UUID id);
    PublicCardResponse getPublicCardBySlug(String slug);
    PublicCardResponse verifyPasscodeAndGetCard(String slug, String passcode);
    WishResponse addWish(String slug, CreateWishRequest request);
    List<WishResponse> getCardWishes(String slug);
    CardResponse mapToResponse(Card card);
    WishResponse mapWishToResponse(CardWish wish);
}
