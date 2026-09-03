package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.CreatePaymentRequest;
import com.invitation.backend.dto.response.PaymentOrderResponse;
import com.invitation.backend.dto.response.TransactionDto;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.security.UserDetailsImpl;
import com.invitation.backend.service.PaymentService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody CreatePaymentRequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        PaymentOrderResponse response = paymentService.createPaymentOrder(user, request);
        return ResponseEntity.ok(ApiResponse.ok("Payment order created", response));
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<TransactionDto>>> getMyTransactions(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        org.springframework.data.domain.Page<TransactionDto> result = paymentService.getUserTransactions(user, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/orders/{orderCode}/status")
    public ResponseEntity<ApiResponse<TransactionDto>> getOrderStatus(
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Transaction transaction = paymentService.getTransactionByOrderCode(orderCode);
        
        // Optional verification: ensure caller owns the transaction or is admin
        if (userDetails != null && !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            if (!transaction.getUser().getId().equals(userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Báº¡n khÃ´ng cÃ³ quyá»n xem Ä‘Æ¡n hÃ ng nÃ y"));
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(TransactionDto.fromEntity(transaction)));
    }

    @GetMapping("/orders/{orderCode}/details")
    public ResponseEntity<ApiResponse<PaymentOrderResponse>> getOrderDetails(
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Transaction transaction = paymentService.getTransactionByOrderCode(orderCode);

        if (userDetails != null && !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            if (!transaction.getUser().getId().equals(userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Báº¡n khÃ´ng cÃ³ quyá»n xem thÃ´ng tin Ä‘Æ¡n hÃ ng nÃ y"));
            }
        }

        PaymentOrderResponse response = paymentService.getPaymentOrderDetails(orderCode);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/simulate-success/{orderCode}")
    public ResponseEntity<ApiResponse<Boolean>> simulateSuccessPayment(
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        boolean isAdmin = userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));

        if (!paymentService.isSandbox() && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("TÃ­nh nÄƒng giáº£ láº­p thanh toÃ¡n Ä‘Ã£ bá»‹ khÃ³a á»Ÿ mÃ´i trÆ°á»ng Production"));
        }

        Transaction transaction = paymentService.getTransactionByOrderCode(orderCode);
        boolean confirmed = paymentService.confirmPayment(
                orderCode,
                transaction.getAmount(),
                "{\"simulation\": true, \"note\": \"Simulated bank transfer completed\"}"
        );

        return ResponseEntity.ok(ApiResponse.ok("Payment simulated successfully", confirmed));
    }

    @PostMapping("/orders/{orderCode}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelUserOrder(
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Transaction transaction = paymentService.getTransactionByOrderCode(orderCode);

        if (userDetails != null && !userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
            if (!transaction.getUser().getId().equals(userDetails.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Báº¡n khÃ´ng cÃ³ quyá»n thao tÃ¡c trÃªn Ä‘Æ¡n hÃ ng nÃ y"));
            }
        }

        boolean success = paymentService.cancelTransaction(orderCode);
        return ResponseEntity.ok(ApiResponse.ok("ÄÃ£ há»§y Ä‘Æ¡n náº¡p tiá»n thÃ nh cÃ´ng", success));
    }

    /**
     * Webhook Handler supporting SePay HMAC-SHA256 Signature and API Key methods.
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Boolean>> handleWebhook(
            @RequestBody(required = false) String rawBody,
            @RequestHeader(value = "X-SePay-Signature", required = false) String xSepaySignature,
            @RequestHeader(value = "x-sepay-signature", required = false) String xSepaySignatureLower,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String xSepayTimestamp,
            @RequestHeader(value = "x-sepay-timestamp", required = false) String xSepayTimestampLower,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-API-KEY", required = false) String xApiKey,
            @RequestHeader(value = "x-api-key", required = false) String xApiKeyLower,
            @RequestParam(value = "token", required = false) String queryToken,
            HttpServletRequest request) {

        log.info("Received Payment Webhook request from IP: {}", request.getRemoteAddr());
        String signature = xSepaySignature != null ? xSepaySignature : xSepaySignatureLower;
        String timestamp = xSepayTimestamp != null ? xSepayTimestamp : xSepayTimestampLower;
        log.info("Webhook Signature: {}, Timestamp: {}, RawBody length: {}", signature, timestamp, rawBody != null ? rawBody.length() : 0);

        String configuredToken = paymentService.getWebhookToken();
        if (configuredToken != null && !configuredToken.isBlank()) {
            boolean isValidAuth = false;

            // 1. Check HMAC-SHA256 Signature (Recommended by SePay)
            if (signature != null && !signature.isBlank() && timestamp != null && !timestamp.isBlank()) {
                String payloadToSign = timestamp + "." + (rawBody != null ? rawBody : "");
                String calculatedHex = computeHmacSha256(payloadToSign, configuredToken.trim());
                String cleanSignature = signature.replaceFirst("^(?i)sha256=", "").trim();

                if (MessageDigest.isEqual(cleanSignature.getBytes(StandardCharsets.UTF_8), calculatedHex.getBytes(StandardCharsets.UTF_8))) {
                    isValidAuth = true;
                    log.info("HMAC-SHA256 signature verified successfully for SePay webhook");
                } else {
                    log.warn("HMAC-SHA256 signature mismatch. Received: '{}', Calculated: '{}'", signature, calculatedHex);
                }
            }

            // 2. Fallback check for API Key / Bearer Token / Query Token
            if (!isValidAuth) {
                String receivedToken = null;
                if (authHeader != null && !authHeader.isBlank()) {
                    receivedToken = authHeader.replaceFirst("^(?i)(apikey|bearer)\\s+", "").trim();
                } else if (xApiKey != null && !xApiKey.isBlank()) {
                    receivedToken = xApiKey.trim();
                } else if (xApiKeyLower != null && !xApiKeyLower.isBlank()) {
                    receivedToken = xApiKeyLower.trim();
                } else if (queryToken != null && !queryToken.isBlank()) {
                    receivedToken = queryToken.trim();
                }

                if (receivedToken != null && !receivedToken.isBlank()) {
                    if (MessageDigest.isEqual(receivedToken.trim().getBytes(StandardCharsets.UTF_8),
                            configuredToken.trim().getBytes(StandardCharsets.UTF_8))) {
                        isValidAuth = true;
                    }
                }
            }

            if (!isValidAuth) {
                log.warn("Unauthorized webhook attempt from IP {}. Signature: '{}', AuthHeader: '{}'",
                        request.getRemoteAddr(), signature, authHeader);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Unauthorized: Webhook HMAC Signature hoáº·c Token khÃ´ng há»£p lá»‡."));
            }
        }

        if (rawBody == null || rawBody.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok("Ping / Test Webhook received successfully", true));
        }

        Map<String, Object> payload = Collections.emptyMap();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            payload = objectMapper.readValue(rawBody, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON payload: {}", e.getMessage());
        }

        if (payload.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.ok("Ping / Empty Webhook received successfully", true));
        }

        // Parse order code from payload (supports SePay, Casso, PayOS, Custom)
        String orderCode = null;

        if (payload.containsKey("orderCode")) {
            orderCode = String.valueOf(payload.get("orderCode"));
        }

        // SePay / Casso syntax: "content" or "description" containing "INV..."
        if (orderCode == null || orderCode.isBlank()) {
            String content = (String) payload.getOrDefault("content", payload.getOrDefault("description", ""));
            if (content != null && !content.isBlank()) {
                Matcher matcher = Pattern.compile("INV[0-9]{4,}", Pattern.CASE_INSENSITIVE).matcher(content);
                if (matcher.find()) {
                    orderCode = matcher.group().toUpperCase();
                } else {
                    Matcher generalMatcher = Pattern.compile("INV[0-9A-Za-z]+", Pattern.CASE_INSENSITIVE).matcher(content);
                    if (generalMatcher.find()) {
                        orderCode = generalMatcher.group().toUpperCase();
                    }
                }
            }
        }

        if (orderCode == null || orderCode.isBlank()) {
            log.info("Received Test Webhook or non-INV payload from SePay: {}", payload);
            return ResponseEntity.ok(ApiResponse.ok("Test Webhook received successfully (no orderCode match)", true));
        }

        // Extract received amount (SePay: transferAmount, Casso: amount)
        Long transferAmount = null;
        Object amountObj = payload.getOrDefault("transferAmount", payload.getOrDefault("amount", null));
        if (amountObj instanceof Number number) {
            transferAmount = number.longValue();
        } else if (amountObj instanceof String str) {
            try {
                transferAmount = Long.parseLong(str);
            } catch (Exception ignored) {}
        }

        try {
            paymentService.confirmPayment(orderCode, transferAmount, rawBody);
            log.info("Webhook successfully processed order: {}", orderCode);
            return ResponseEntity.ok(ApiResponse.ok("Webhook processed successfully", true));
        } catch (IllegalArgumentException e) {
            log.warn("Webhook received orderCode '{}' but not found/already completed: {}", orderCode, e.getMessage());
            return ResponseEntity.ok(ApiResponse.ok("Webhook received: " + e.getMessage(), false));
        }
    }

    private String computeHmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.error("Failed to calculate HMAC-SHA256: {}", e.getMessage());
            return "";
        }
    }
}

