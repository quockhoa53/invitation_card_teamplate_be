package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.CreatePaymentRequest;
import com.invitation.backend.dto.response.PaymentOrderResponse;
import com.invitation.backend.dto.response.TransactionDto;
import com.invitation.backend.entity.Card;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import com.invitation.backend.repository.CardRepository;
import com.invitation.backend.repository.TransactionRepository;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.service.PaymentService;
import com.invitation.backend.service.QrCodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final QrCodeGeneratorService qrCodeGeneratorService;

    @Value("${app.vietqr.bank-id}")
    private String bankId;

    @Value("${app.vietqr.bank-name}")
    private String bankName;

    @Value("${app.vietqr.account-no}")
    private String accountNo;

    @Value("${app.vietqr.account-name}")
    private String accountName;

    @Value("${app.payment.sandbox:false}")
    private boolean sandbox;

    @Value("${app.payment.webhook.token}")
    private String webhookToken;

    @Override
    public boolean isSandbox() {
        return sandbox;
    }

    @Override
    public String getWebhookToken() {
        return webhookToken;
    }

    @Override
    @Transactional
    public PaymentOrderResponse createPaymentOrder(User user, CreatePaymentRequest request) {
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("Số tiền nạp phải lớn hơn 0 VNĐ");
        }

        Card card = null;
        if (request.getCardId() != null) {
            card = cardRepository.findById(request.getCardId())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thiệp được chỉ định"));
        }

        String orderCode;
        int attempts = 0;
        do {
            orderCode = "INV" + (100000000 + new Random().nextInt(900000000));
            attempts++;
            if (attempts > 10) {
                orderCode = "INV" + System.currentTimeMillis();
                break;
            }
        } while (transactionRepository.findByOrderCode(orderCode).isPresent());

        Transaction transaction = Transaction.builder()
                .user(user)
                .card(card)
                .orderCode(orderCode)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "VIETQR")
                .amount(request.getAmount())
                .status(Transaction.Status.PENDING)
                .build();

        transactionRepository.save(transaction);

        String transferContent = orderCode;
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String vietQrUrl = String.format(
                "https://qr.sepay.vn/img?bank=%s&acc=%s&amount=%d&des=%s",
                bankName, accountNo, request.getAmount(), encodedContent
        );

        String qrContent = String.format("2|99|%s|%s|%s|0|0|%d|%s|transfer_myqr",
                accountNo, accountName, bankId, request.getAmount(), transferContent);
        String qrCodeBase64 = qrCodeGeneratorService.generateQrCodeBase64(qrContent, 280, 280);

        return PaymentOrderResponse.builder()
                .orderCode(orderCode)
                .amount(request.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .vietQrUrl(vietQrUrl)
                .qrCodeBase64(qrCodeBase64)
                .bankName(bankName)
                .bankAccountNo(accountNo)
                .accountHolder(accountName)
                .transferContent(transferContent)
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Override
    public PaymentOrderResponse getPaymentOrderDetails(String orderCode) {
        Transaction transaction = getTransactionByOrderCode(orderCode);

        String transferContent = transaction.getOrderCode();
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedName = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        String vietQrUrl = String.format(
                "https://qr.sepay.vn/img?bank=%s&acc=%s&amount=%d&des=%s",
                bankName, accountNo, transaction.getAmount(), encodedContent
        );

        String qrContent = String.format("2|99|%s|%s|%s|0|0|%d|%s|transfer_myqr",
                accountNo, accountName, bankId, transaction.getAmount(), transferContent);
        String qrCodeBase64 = qrCodeGeneratorService.generateQrCodeBase64(qrContent, 280, 280);

        return PaymentOrderResponse.builder()
                .orderCode(transaction.getOrderCode())
                .amount(transaction.getAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .vietQrUrl(vietQrUrl)
                .qrCodeBase64(qrCodeBase64)
                .bankName(bankName)
                .bankAccountNo(accountNo)
                .accountHolder(accountName)
                .transferContent(transferContent)
                .status(transaction.getStatus().name())
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    @Override
    public Transaction getTransactionByOrderCode(String orderCode) {
        return transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn giao dịch: " + orderCode));
    }

    @Override
    @Transactional
    public boolean confirmPayment(String orderCode, Long receivedAmount, String gatewayPayload) {
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn nạp tiền: " + orderCode));

        // Idempotency: If already confirmed, don't credit user twice
        if (transaction.getStatus() == Transaction.Status.SUCCESS) {
            log.info("Transaction {} already confirmed SUCCESS. Skipping.", orderCode);
            return true;
        }

        // Validate amount if receivedAmount is provided
        if (receivedAmount != null && receivedAmount < transaction.getAmount()) {
            log.warn("Received amount {} is less than order amount {} for order {}", receivedAmount, transaction.getAmount(), orderCode);
            transaction.setGatewayPayload(gatewayPayload);
            transactionRepository.save(transaction);
            throw new IllegalArgumentException(String.format("Số tiền chuyển (%d đ) nhỏ hơn số tiền yêu cầu (%d đ)", receivedAmount, transaction.getAmount()));
        }

        // Atomic DB update to prevent concurrent double crediting race conditions
        LocalDateTime now = LocalDateTime.now();
        int rowsUpdated = transactionRepository.atomicMarkSuccess(orderCode, Transaction.Status.SUCCESS, now, gatewayPayload);
        if (rowsUpdated == 0) {
            log.info("Concurrent webhook detected for order {}. Another thread already marked it SUCCESS.", orderCode);
            return true;
        }

        // Atomic user credits increment
        userRepository.atomicAddCredits(transaction.getUser().getId(), transaction.getAmount());

        log.info("Successfully confirmed order {} and atomically credited {} VND to user ID {}",
                orderCode, transaction.getAmount(), transaction.getUser().getId());
        return true;
    }

    @Override
    @Transactional
    public boolean approveManualTransaction(String orderCode, User adminUser) {
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch: " + orderCode));

        if (transaction.getStatus() == Transaction.Status.SUCCESS) {
            throw new IllegalStateException("Giao dịch này đã được duyệt thành công trước đó.");
        }

        String note = String.format("{\"manualApproval\": true, \"admin\": \"%s\", \"approvedAt\": \"%s\"}",
                adminUser.getEmail(), LocalDateTime.now());

        return confirmPayment(orderCode, transaction.getAmount(), note);
    }

    @Override
    @Transactional
    public boolean cancelTransaction(String orderCode) {
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch: " + orderCode));

        if (transaction.getStatus() == Transaction.Status.SUCCESS) {
            throw new IllegalStateException("Không thể hủy giao dịch đã nạp tiền thành công.");
        }

        transaction.setStatus(Transaction.Status.CANCELLED);
        transaction.setGatewayPayload("{\"cancelled\": true, \"time\": \"" + LocalDateTime.now() + "\"}");
        transactionRepository.save(transaction);
        return true;
    }

    @Override
    public Page<TransactionDto> getAdminTransactions(String statusStr, String search, Pageable pageable) {
        Transaction.Status status = null;
        if (statusStr != null && !statusStr.equalsIgnoreCase("ALL") && !statusStr.isBlank()) {
            try {
                status = Transaction.Status.valueOf(statusStr.toUpperCase());
            } catch (Exception ignored) {}
        }

        String keyword = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Transaction> page;
        if (status == null && keyword == null) {
            page = transactionRepository.findAllWithUser(pageable);
        } else if (status != null && keyword == null) {
            page = transactionRepository.findAllByStatus(status, pageable);
        } else {
            page = transactionRepository.findAllFiltered(status, keyword, pageable);
        }
        return page.map(TransactionDto::fromEntity);
    }

    @Override
    public Page<TransactionDto> getUserTransactions(User user, Pageable pageable) {
        Page<Transaction> page = transactionRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return page.map(TransactionDto::fromEntity);
    }
}
