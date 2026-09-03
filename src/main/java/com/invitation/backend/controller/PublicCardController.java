package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.*;
import com.invitation.backend.dto.response.*;
import com.invitation.backend.service.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/cards")
@RequiredArgsConstructor
public class PublicCardController {

    private final CardService cardService;

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<PublicCardResponse>> getPublicCard(@PathVariable String slug) {
        PublicCardResponse response = cardService.getPublicCardBySlug(slug);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{slug}/verify-passcode")
    public ResponseEntity<ApiResponse<PublicCardResponse>> verifyPasscode(
            @PathVariable String slug,
            @Valid @RequestBody VerifyPasscodeRequest request) {
        PublicCardResponse response = cardService.verifyPasscodeAndGetCard(slug, request.getPasscode());
        return ResponseEntity.ok(ApiResponse.ok("Passcode verified successfully", response));
    }

    @PostMapping("/{slug}/wishes")
    public ResponseEntity<ApiResponse<WishResponse>> addWish(
            @PathVariable String slug,
            @Valid @RequestBody CreateWishRequest request) {
        WishResponse response = cardService.addWish(slug, request);
        return ResponseEntity.ok(ApiResponse.ok("Wish posted successfully", response));
    }

    @GetMapping("/{slug}/wishes")
    public ResponseEntity<ApiResponse<List<WishResponse>>> getWishes(@PathVariable String slug) {
        List<WishResponse> wishes = cardService.getCardWishes(slug);
        return ResponseEntity.ok(ApiResponse.ok(wishes));
    }
}

