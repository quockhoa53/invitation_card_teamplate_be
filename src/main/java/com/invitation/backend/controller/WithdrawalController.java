package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.WithdrawalRequest;
import com.invitation.backend.dto.response.WithdrawalDto;
import com.invitation.backend.entity.User;
import com.invitation.backend.entity.Withdrawal;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.security.UserDetailsImpl;
import com.invitation.backend.service.WithdrawalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WithdrawalController {

    private final WithdrawalService withdrawalService;
    private final UserRepository userRepository;

    // User: Request withdrawal
    @PostMapping("/withdrawals")
    public ResponseEntity<ApiResponse<WithdrawalDto>> requestWithdrawal(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody WithdrawalRequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        WithdrawalDto result = withdrawalService.requestWithdrawal(user, request);
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi yêu cầu rút tiền thành công", result));
    }

    // User: My withdrawals
    @GetMapping("/withdrawals/my")
    public ResponseEntity<ApiResponse<Page<WithdrawalDto>>> getMyWithdrawals(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalDto> result = withdrawalService.getUserWithdrawals(user, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Admin: List all withdrawal requests
    @GetMapping("/admin/withdrawals")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<WithdrawalDto>>> getAdminWithdrawals(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Withdrawal.Status withdrawalStatus = null;
        if (status != null && !status.equalsIgnoreCase("ALL") && !status.isBlank()) {
            try {
                withdrawalStatus = Withdrawal.Status.valueOf(status.toUpperCase());
            } catch (Exception ignored) {}
        }
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalDto> result = withdrawalService.getAdminWithdrawals(withdrawalStatus, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Admin: Approve payout (Admin has transferred bank money to user)
    @PostMapping("/admin/withdrawals/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawalDto>> approveWithdrawal(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl adminDetails,
            @RequestBody(required = false) Map<String, String> body) {
        User admin = userRepository.findById(adminDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        String note = body != null ? body.get("note") : null;
        WithdrawalDto result = withdrawalService.approveWithdrawal(id, admin, note);
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt chuyển tiền cho khách hàng thành công", result));
    }

    // Admin: Reject payout (Refund locked money back to user)
    @PostMapping("/admin/withdrawals/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<WithdrawalDto>> rejectWithdrawal(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetailsImpl adminDetails,
            @RequestBody(required = false) Map<String, String> body) {
        User admin = userRepository.findById(adminDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        String reason = body != null ? body.get("reason") : "Thông tin ngân hàng không hợp lệ";
        WithdrawalDto result = withdrawalService.rejectWithdrawal(id, admin, reason);
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối và hoàn tiền lại ví người dùng", result));
    }
}
