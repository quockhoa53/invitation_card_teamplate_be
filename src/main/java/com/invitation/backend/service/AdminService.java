package com.invitation.backend.service;

import com.invitation.backend.dto.response.AdminStatsResponse;
import com.invitation.backend.dto.response.UserDto;
import com.invitation.backend.entity.Role;
import com.invitation.backend.entity.Transaction;

import java.util.List;
import java.util.UUID;

public interface AdminService {
    AdminStatsResponse getAdminStats();
    List<UserDto> getAllUsers();
    UserDto toggleUserStatus(UUID userId);
    UserDto updateUserRole(UUID userId, Role newRole);
    List<Transaction> getAllTransactions();
}
