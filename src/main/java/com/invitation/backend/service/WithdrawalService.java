package com.invitation.backend.service;

import com.invitation.backend.dto.request.WithdrawalRequest;
import com.invitation.backend.dto.response.WithdrawalDto;
import com.invitation.backend.entity.User;
import com.invitation.backend.entity.Withdrawal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WithdrawalService {

    WithdrawalDto requestWithdrawal(User user, WithdrawalRequest request);

    Page<WithdrawalDto> getUserWithdrawals(User user, Pageable pageable);

    Page<WithdrawalDto> getAdminWithdrawals(Withdrawal.Status status, String search, Pageable pageable);

    WithdrawalDto approveWithdrawal(UUID withdrawalId, User adminUser, String note);

    WithdrawalDto rejectWithdrawal(UUID withdrawalId, User adminUser, String reason);
}
