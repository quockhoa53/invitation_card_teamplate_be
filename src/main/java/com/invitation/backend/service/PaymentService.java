package com.invitation.backend.service;

import com.invitation.backend.dto.request.CreatePaymentRequest;
import com.invitation.backend.dto.response.PaymentOrderResponse;
import com.invitation.backend.dto.response.TransactionDto;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentOrderResponse createPaymentOrder(User user, CreatePaymentRequest request);
    PaymentOrderResponse getPaymentOrderDetails(String orderCode);
    Transaction getTransactionByOrderCode(String orderCode);
    boolean confirmPayment(String orderCode, Long receivedAmount, String gatewayPayload);
    boolean approveManualTransaction(String orderCode, User adminUser);
    boolean cancelTransaction(String orderCode);
    PaymentOrderResponse settleUnderpaidToWallet(String orderCode, User user);
    PaymentOrderResponse getSupplementOrderDetails(String orderCode, User user);
    Page<TransactionDto> getAdminTransactions(String statusStr, String search, Pageable pageable);
    Page<TransactionDto> getUserTransactions(User user, Pageable pageable);
    String getWebhookToken();
    boolean isSandbox();
}
