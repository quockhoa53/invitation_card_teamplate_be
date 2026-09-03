package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.response.TransactionDto;
import com.invitation.backend.entity.User;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.security.UserDetailsImpl;
import com.invitation.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN')")
public class AdminTransactionController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getTransactions(
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TransactionDto> result = paymentService.getAdminTransactions(status, search, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{orderCode}/approve")
    public ResponseEntity<ApiResponse<Boolean>> approveTransaction(
            @PathVariable String orderCode,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        User adminUser = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));

        boolean success = paymentService.approveManualTransaction(orderCode, adminUser);
        return ResponseEntity.ok(ApiResponse.ok("ÄÃ£ duyá»‡t náº¡p tiá»n thá»§ cÃ´ng thÃ nh cÃ´ng", success));
    }

    @PostMapping("/{orderCode}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelTransaction(@PathVariable String orderCode) {
        boolean success = paymentService.cancelTransaction(orderCode);
        return ResponseEntity.ok(ApiResponse.ok("ÄÃ£ há»§y giao dá»‹ch", success));
    }
}

