package com.invitation.backend.service.impl;

import com.invitation.backend.dto.response.AdminStatsResponse;
import com.invitation.backend.dto.response.UserDto;
import com.invitation.backend.entity.Role;
import com.invitation.backend.entity.Transaction;
import com.invitation.backend.entity.User;
import com.invitation.backend.repository.CardRepository;
import com.invitation.backend.repository.TemplateRepository;
import com.invitation.backend.repository.TransactionRepository;
import com.invitation.backend.repository.User2FARepository;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.service.AdminService;
import com.invitation.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final TemplateRepository templateRepository;
    private final TransactionRepository transactionRepository;
    private final User2FARepository user2FARepository;
    private final AuthService authService;

    @Override
    public AdminStatsResponse getAdminStats() {
        long totalUsers = userRepository.count();
        long totalCards = cardRepository.count();
        long totalTemplates = templateRepository.count();
        long totalRevenue = transactionRepository.getTotalRevenue() != null ? transactionRepository.getTotalRevenue() : 0L;
        long totalTransactions = transactionRepository.countRevenueTransactions() != null ? transactionRepository.countRevenueTransactions() : 0L;
        long activeTemplates = templateRepository.countByIsActiveTrue();
        long publishedCards = cardRepository.countByIsPublishedTrue();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        List<AdminStatsResponse.TransactionDto> recentTransactions = transactionRepository.findAll(pageable).stream()
                .map(t -> AdminStatsResponse.TransactionDto.builder()
                        .orderCode(t.getOrderCode())
                        .userEmail(t.getUser() != null ? t.getUser().getEmail() : "N/A")
                        .userName(t.getUser() != null ? t.getUser().getFullName() : "N/A")
                        .amount(t.getAmount())
                        .paymentMethod(t.getPaymentMethod())
                        .type(t.getType())
                        .status(t.getStatus().name())
                        .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "")
                        .build())
                .collect(Collectors.toList());

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalCards(totalCards)
                .totalTemplates(totalTemplates)
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .activeTemplatesCount(activeTemplates)
                .publishedCardsCount(publishedCards)
                .recentTransactions(recentTransactions)
                .build();
    }

    @Override
    public List<UserDto> getAllUsers() {
        return userRepository.findAll(Sort.by("createdAt").descending()).stream()
                .map(u -> authService.mapToDto(u, user2FARepository.findByUser(u).map(t -> t.getIsEnabled()).orElse(false)))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDto toggleUserStatus(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setIsActive(!user.getIsActive());
        user = userRepository.save(user);
        return authService.mapToDto(user, user2FARepository.findByUser(user).map(t -> t.getIsEnabled()).orElse(false));
    }

    @Override
    @Transactional
    public UserDto updateUserRole(UUID userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setRole(newRole);
        user = userRepository.save(user);
        return authService.mapToDto(user, user2FARepository.findByUser(user).map(t -> t.getIsEnabled()).orElse(false));
    }

    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll(Sort.by("createdAt").descending());
    }
}
