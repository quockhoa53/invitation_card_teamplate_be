package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.CreatePaymentRequest;
import com.invitation.backend.dto.response.PaymentOrderResponse;
import com.invitation.backend.dto.response.TransactionDto;
import com.invitation.backend.entity.Card;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import com.invitation.backend.entity.Withdrawal;
import com.invitation.backend.repository.CardRepository;
import com.invitation.backend.repository.TransactionRepository;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.repository.WithdrawalRepository;
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
    private final WithdrawalRepository withdrawalRepository;

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

    public long calculateBonus(long amount) {
        if (amount >= 100000) {
            return 30000L;
        } else if (amount >= 50000) {
            return 10000L;
        }
        return 0L;
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

        long bonusAmount = calculateBonus(request.getAmount());

        Transaction transaction = Transaction.builder()
                .user(user)
                .card(card)
                .orderCode(orderCode)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "VIETQR")
                .amount(request.getAmount())
                .bonusAmount(bonusAmount)
                .type("DEPOSIT")
                .status(Transaction.Status.PENDING)
                .build();

        transactionRepository.save(transaction);

        String transferContent = orderCode;
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
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
                .bonusAmount(bonusAmount)
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
                .bonusAmount(transaction.getBonusAmount())
                .actualAmount(transaction.getActualAmount())
                .missingAmount(transaction.getMissingAmount())
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

        // Check if this is a supplement payment for an underpaid order
        long effectiveReceived = receivedAmount != null ? receivedAmount : transaction.getAmount();
        if (transaction.getStatus() == Transaction.Status.UNDERPAID) {
            long prevReceived = transaction.getActualAmount() != null ? transaction.getActualAmount() : 0L;
            effectiveReceived += prevReceived;
        }

        // Case 1: UNDERPAID (Khách chuyển thiếu)
        if (effectiveReceived < transaction.getAmount()) {
            long missing = transaction.getAmount() - effectiveReceived;
            log.warn("Underpaid detected! Received {} vs required {} for order {}. Missing: {}",
                    effectiveReceived, transaction.getAmount(), orderCode, missing);

            transaction.setStatus(Transaction.Status.UNDERPAID);
            transaction.setActualAmount(effectiveReceived);
            transaction.setMissingAmount(missing);
            transaction.setGatewayPayload(gatewayPayload);
            transactionRepository.save(transaction);
            return false;
        }

        // Case 2 & 3: OVERPAID or EXACT (Khách chuyển dư hoặc chuyển đủ)
        long finalDepositAmount = effectiveReceived;
        long bonusAmount = calculateBonus(finalDepositAmount);

        transaction.setAmount(finalDepositAmount);
        transaction.setActualAmount(finalDepositAmount);
        transaction.setBonusAmount(bonusAmount);
        transaction.setMissingAmount(0L);
        transaction.setStatus(Transaction.Status.SUCCESS);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setGatewayPayload(gatewayPayload);
        transactionRepository.save(transaction);

        // Atomically credit realBalance and bonusBalance
        userRepository.atomicAddDepositWithBonus(transaction.getUser().getId(), finalDepositAmount, bonusAmount);

        log.info("Successfully confirmed order {} and atomically credited {} VND real + {} VND bonus to user ID {}",
                orderCode, finalDepositAmount, bonusAmount, transaction.getUser().getId());
        return true;
    }

    @Override
    @Transactional
    public PaymentOrderResponse settleUnderpaidToWallet(String orderCode, User user) {
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn nạp tiền: " + orderCode));

        if (!transaction.getUser().getId().equals(user.getId()) && !user.getRole().name().contains("ADMIN")) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác trên đơn hàng này");
        }

        if (transaction.getStatus() != Transaction.Status.UNDERPAID) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chuyển thiếu");
        }

        long actualAmount = transaction.getActualAmount() != null ? transaction.getActualAmount() : 0L;
        if (actualAmount <= 0) {
            throw new IllegalStateException("Chưa ghi nhận số tiền chuyển của đơn hàng này");
        }

        // Credit actualAmount directly to user's realBalance (0 bonus because underpaid)
        userRepository.atomicAddDepositWithBonus(transaction.getUser().getId(), actualAmount, 0L);

        transaction.setStatus(Transaction.Status.SETTLED_TO_WALLET);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setGatewayPayload((transaction.getGatewayPayload() != null ? transaction.getGatewayPayload() : "") +
                " | Settled to wallet: " + actualAmount + " VND at " + LocalDateTime.now());
        transactionRepository.save(transaction);

        log.info("Settled underpaid order {} of {} VND to wallet for user {}",
                orderCode, actualAmount, transaction.getUser().getEmail());

        return getPaymentOrderDetails(orderCode);
    }

    @Override
    public PaymentOrderResponse getSupplementOrderDetails(String orderCode, User user) {
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn nạp tiền: " + orderCode));

        if (!transaction.getUser().getId().equals(user.getId()) && !user.getRole().name().contains("ADMIN")) {
            throw new IllegalArgumentException("Bạn không có quyền thao tác trên đơn hàng này");
        }

        if (transaction.getStatus() != Transaction.Status.UNDERPAID) {
            throw new IllegalStateException("Đơn hàng không ở trạng thái chuyển thiếu");
        }

        long missing = transaction.getMissingAmount() != null ? transaction.getMissingAmount() : 0L;
        if (missing <= 0) {
            throw new IllegalStateException("Đơn hàng này không còn số tiền thiếu cần nạp");
        }

        String transferContent = orderCode; // Use same base orderCode so VietQR auto matches
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String vietQrUrl = String.format(
                "https://qr.sepay.vn/img?bank=%s&acc=%s&amount=%d&des=%s",
                bankName, accountNo, missing, encodedContent
        );

        String qrContent = String.format("2|99|%s|%s|%s|0|0|%d|%s|transfer_myqr",
                accountNo, accountName, bankId, missing, transferContent);
        String qrCodeBase64 = qrCodeGeneratorService.generateQrCodeBase64(qrContent, 280, 280);

        return PaymentOrderResponse.builder()
                .orderCode(transaction.getOrderCode())
                .amount(missing) // The missing amount to pay
                .actualAmount(transaction.getActualAmount())
                .missingAmount(missing)
                .bonusAmount(transaction.getBonusAmount())
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
    @Transactional
    public boolean approveManualTransaction(String orderCode, User adminUser) {
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch: " + orderCode));

        if (transaction.getStatus() == Transaction.Status.SUCCESS) {
            throw new IllegalStateException("Giao dịch này đã được duyệt thành công trước đó.");
        }

        // Special handling for WITHDRAWAL approval
        if (orderCode.startsWith("WDR") || "WITHDRAWAL".equalsIgnoreCase(transaction.getType())) {
            String withdrawalPrefix = orderCode.substring(3).toLowerCase();
            withdrawalRepository.findAll().stream()
                    .filter(w -> w.getId().toString().replace("-", "").toLowerCase().startsWith(withdrawalPrefix))
                    .findFirst()
                    .ifPresent(w -> {
                        w.setStatus(Withdrawal.Status.APPROVED);
                        w.setProcessedAt(LocalDateTime.now());
                        w.setAdminNote(String.format("Duyệt chuyển tiền bởi admin %s", adminUser.getEmail()));
                        withdrawalRepository.save(w);
                    });

            transaction.setStatus(Transaction.Status.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setGatewayPayload(String.format("{\"manualApproval\": true, \"admin\": \"%s\", \"approvedAt\": \"%s\"}",
                    adminUser.getEmail(), LocalDateTime.now()));
            transactionRepository.save(transaction);
            return true;
        }

        long amountToApprove = transaction.getActualAmount() != null && transaction.getActualAmount() > 0
                ? transaction.getActualAmount()
                : transaction.getAmount();

        String note = String.format("{\"manualApproval\": true, \"admin\": \"%s\", \"approvedAt\": \"%s\"}",
                adminUser.getEmail(), LocalDateTime.now());

        return confirmPayment(orderCode, amountToApprove, note);
    }

    @Override
    @Transactional
    public boolean cancelTransaction(String orderCode) {
        Transaction transaction = transactionRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch: " + orderCode));

        if (transaction.getStatus() == Transaction.Status.SUCCESS) {
            throw new IllegalStateException("Không thể hủy giao dịch đã hoàn tất thành công.");
        }

        // Special handling for WITHDRAWAL cancellation / rejection
        if (orderCode.startsWith("WDR") || "WITHDRAWAL".equalsIgnoreCase(transaction.getType())) {
            String withdrawalPrefix = orderCode.substring(3).toLowerCase();
            withdrawalRepository.findAll().stream()
                    .filter(w -> w.getStatus() == Withdrawal.Status.PENDING &&
                            w.getId().toString().replace("-", "").toLowerCase().startsWith(withdrawalPrefix))
                    .findFirst()
                    .ifPresent(w -> {
                        w.setStatus(Withdrawal.Status.REJECTED);
                        w.setAdminNote("Admin từ chối giao dịch rút tiền");
                        w.setProcessedAt(LocalDateTime.now());
                        withdrawalRepository.save(w);

                        // Refund money back to user real balance
                        userRepository.atomicRefundWithdrawal(w.getUser().getId(), w.getAmount());

                        // Create REFUND transaction log (+money back)
                        String refundOrderCode = "REF" + w.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
                        if (transactionRepository.findByOrderCode(refundOrderCode).isEmpty()) {
                            Transaction refundTx = Transaction.builder()
                                    .user(w.getUser())
                                    .orderCode(refundOrderCode)
                                    .paymentMethod("WALLET")
                                    .amount(w.getAmount())
                                    .type("REFUND")
                                    .status(Transaction.Status.SUCCESS)
                                    .completedAt(LocalDateTime.now())
                                    .gatewayPayload(String.format("{\"refundFor\": \"%s\", \"reason\": \"Hủy giao dịch rút tiền từ Quản Lý Giao Dịch\"}", orderCode))
                                    .build();
                            transactionRepository.save(refundTx);
                        }
                    });

            transaction.setStatus(Transaction.Status.CANCELLED);
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setGatewayPayload("{\"cancelled\": true, \"reason\": \"Admin từ chối yêu cầu rút tiền\", \"time\": \"" + LocalDateTime.now() + "\"}");
            transactionRepository.save(transaction);
            return true;
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
