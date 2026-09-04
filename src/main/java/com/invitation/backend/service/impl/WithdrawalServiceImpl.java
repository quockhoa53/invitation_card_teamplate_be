package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.WithdrawalRequest;
import com.invitation.backend.dto.response.WithdrawalDto;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import com.invitation.backend.entity.Withdrawal;
import com.invitation.backend.repository.TransactionRepository;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.repository.WithdrawalRepository;
import com.invitation.backend.service.WithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalServiceImpl implements WithdrawalService {

    private final WithdrawalRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public WithdrawalDto requestWithdrawal(User user, WithdrawalRequest request) {
        long withdrawAmount = request.getAmount();

        // Refresh user from DB
        User currentUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        long realBalance = currentUser.getRealBalance() != null ? currentUser.getRealBalance() : 0L;

        if (realBalance < withdrawAmount) {
            throw new IllegalArgumentException(String.format(
                    "Số dư khả dụng rút (%d đ) không đủ để rút %d đ. (Lưu ý: Tiền khuyến mãi chỉ dùng để mua thiệp, không được rút ra tiền mặt).",
                    realBalance, withdrawAmount
            ));
        }

        // 1. Atomically lock money from user's real balance
        int rows = userRepository.atomicLockForWithdrawal(user.getId(), withdrawAmount);
        if (rows == 0) {
            throw new IllegalStateException("Số dư khả dụng thay đổi. Vui lòng thử lại!");
        }

        // 2. BONUS CLAWBACK: Revoke remaining promotional bonus balance on withdrawal
        userRepository.atomicClawbackBonus(user.getId());

        // 3. Create Withdrawal record
        Withdrawal withdrawal = Withdrawal.builder()
                .user(currentUser)
                .amount(withdrawAmount)
                .bankName(request.getBankName().trim())
                .accountNumber(request.getAccountNumber().trim())
                .accountHolder(request.getAccountHolder().trim().toUpperCase())
                .status(Withdrawal.Status.PENDING)
                .build();

        withdrawal = withdrawalRepository.save(withdrawal);

        // 4. Create Transaction audit record
        String orderCode = "WDR" + withdrawal.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
        Transaction transaction = Transaction.builder()
                .user(currentUser)
                .orderCode(orderCode)
                .paymentMethod("BANK_TRANSFER")
                .amount(withdrawAmount)
                .type("WITHDRAWAL")
                .status(Transaction.Status.PENDING)
                .gatewayPayload(String.format("{\"bank\": \"%s\", \"acc\": \"%s\", \"name\": \"%s\", \"withdrawalId\": \"%s\"}",
                        withdrawal.getBankName(), withdrawal.getAccountNumber(), withdrawal.getAccountHolder(), withdrawal.getId()))
                .build();

        transactionRepository.save(transaction);

        log.info("User {} requested withdrawal of {} VND. Locked from realBalance and revoked promotional bonus.",
                currentUser.getEmail(), withdrawAmount);

        return WithdrawalDto.fromEntity(withdrawal);
    }

    @Override
    public Page<WithdrawalDto> getUserWithdrawals(User user, Pageable pageable) {
        return withdrawalRepository.findByUserOrderByCreatedAtDesc(user, pageable).map(WithdrawalDto::fromEntity);
    }

    @Override
    public Page<WithdrawalDto> getAdminWithdrawals(Withdrawal.Status status, String search, Pageable pageable) {
        String keyword = (search != null && !search.isBlank()) ? search.trim() : null;
        if (status == null && keyword == null) {
            return withdrawalRepository.findAllByOrderByCreatedAtDesc(pageable).map(WithdrawalDto::fromEntity);
        }
        if (status != null && keyword == null) {
            return withdrawalRepository.findByStatusOrderByCreatedAtDesc(status, pageable).map(WithdrawalDto::fromEntity);
        }
        return withdrawalRepository.findAllFiltered(status, keyword, pageable).map(WithdrawalDto::fromEntity);
    }

    @Override
    @Transactional
    public WithdrawalDto approveWithdrawal(UUID withdrawalId, User adminUser, String note) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu rút tiền"));

        if (withdrawal.getStatus() != Withdrawal.Status.PENDING) {
            throw new IllegalStateException("Yêu cầu rút tiền này đã được xử lý trước đó");
        }

        withdrawal.setStatus(Withdrawal.Status.APPROVED);
        withdrawal.setAdminNote(note != null && !note.isBlank() ? note.trim() : "Admin đã chuyển khoản thành công");
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal = withdrawalRepository.save(withdrawal);

        // Update related transaction if present
        String orderCode = "WDR" + withdrawal.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
        transactionRepository.findByOrderCode(orderCode)
                .ifPresent(t -> {
                    t.setStatus(Transaction.Status.SUCCESS);
                    t.setCompletedAt(LocalDateTime.now());
                    transactionRepository.save(t);
                });

        log.info("Admin {} approved withdrawal {} of {} VND for user {}",
                adminUser.getEmail(), withdrawalId, withdrawal.getAmount(), withdrawal.getUser().getEmail());

        return WithdrawalDto.fromEntity(withdrawal);
    }

    @Override
    @Transactional
    public WithdrawalDto rejectWithdrawal(UUID withdrawalId, User adminUser, String reason) {
        Withdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu rút tiền"));

        if (withdrawal.getStatus() != Withdrawal.Status.PENDING) {
            throw new IllegalStateException("Yêu cầu rút tiền này đã được xử lý trước đó");
        }

        String note = reason != null && !reason.isBlank() ? reason.trim() : "Từ chối yêu cầu rút tiền";
        withdrawal.setStatus(Withdrawal.Status.REJECTED);
        withdrawal.setAdminNote(note);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal = withdrawalRepository.save(withdrawal);

        // 1. Atomically refund locked money back to user's real balance
        userRepository.atomicRefundWithdrawal(withdrawal.getUser().getId(), withdrawal.getAmount());

        // 2. Update related withdrawal transaction to CANCELLED
        String orderCode = "WDR" + withdrawal.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
        transactionRepository.findByOrderCode(orderCode)
                .ifPresent(t -> {
                    t.setStatus(Transaction.Status.CANCELLED);
                    t.setCompletedAt(LocalDateTime.now());
                    t.setGatewayPayload(String.format("{\"cancelled\": true, \"reason\": \"%s\", \"rejectedBy\": \"%s\"}",
                            note.replace("\"", "\\\""), adminUser.getEmail()));
                    transactionRepository.save(t);
                });

        // 3. Create a REFUND transaction log (+money back to customer account)
        String refundOrderCode = "REF" + withdrawal.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
        if (transactionRepository.findByOrderCode(refundOrderCode).isEmpty()) {
            Transaction refundTx = Transaction.builder()
                    .user(withdrawal.getUser())
                    .orderCode(refundOrderCode)
                    .paymentMethod("WALLET")
                    .amount(withdrawal.getAmount())
                    .type("REFUND")
                    .status(Transaction.Status.SUCCESS)
                    .completedAt(LocalDateTime.now())
                    .gatewayPayload(String.format("{\"refundFor\": \"%s\", \"reason\": \"%s\", \"admin\": \"%s\"}",
                            orderCode,
                            note.replace("\"", "\\\""),
                            adminUser.getEmail()))
                    .build();
            transactionRepository.save(refundTx);
        }

        log.info("Admin {} rejected withdrawal {} of {} VND. Refunded to user {} and created REFUND transaction {}",
                adminUser.getEmail(), withdrawalId, withdrawal.getAmount(), withdrawal.getUser().getEmail(), refundOrderCode);

        return WithdrawalDto.fromEntity(withdrawal);
    }

    @jakarta.annotation.PostConstruct
    public void syncHistoricalRejectedWithdrawals() {
        try {
            List<Withdrawal> rejectedList = withdrawalRepository.findAll().stream()
                    .filter(w -> w.getStatus() == Withdrawal.Status.REJECTED)
                    .toList();

            for (Withdrawal w : rejectedList) {
                String orderCode = "WDR" + w.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
                transactionRepository.findByOrderCode(orderCode).ifPresent(t -> {
                    if (t.getStatus() == Transaction.Status.PENDING) {
                        t.setStatus(Transaction.Status.CANCELLED);
                        t.setCompletedAt(w.getProcessedAt() != null ? w.getProcessedAt() : LocalDateTime.now());
                        transactionRepository.save(t);
                        log.info("Synced historical withdrawal transaction {} to CANCELLED", orderCode);
                    }
                });

                String refundCode = "REF" + w.getId().toString().replace("-", "").substring(0, 12).toUpperCase();
                if (transactionRepository.findByOrderCode(refundCode).isEmpty()) {
                    Transaction refundTx = Transaction.builder()
                            .user(w.getUser())
                            .orderCode(refundCode)
                            .paymentMethod("WALLET")
                            .amount(w.getAmount())
                            .type("REFUND")
                            .status(Transaction.Status.SUCCESS)
                            .completedAt(w.getProcessedAt() != null ? w.getProcessedAt() : LocalDateTime.now())
                            .gatewayPayload(String.format("{\"refundFor\": \"%s\", \"reason\": \"%s\"}",
                                    orderCode,
                                    (w.getAdminNote() != null ? w.getAdminNote() : "Hoàn tiền do yêu cầu rút tiền bị từ chối").replace("\"", "\\\"")))
                            .build();
                    transactionRepository.save(refundTx);
                    log.info("Created missing historical REFUND transaction {} for withdrawal {}", refundCode, w.getId());
                }
            }
        } catch (Exception e) {
            log.warn("Error syncing historical rejected withdrawals: {}", e.getMessage());
        }
    }
}
