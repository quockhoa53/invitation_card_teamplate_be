package com.invitation.backend.dto.response;

import com.invitation.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private Boolean isActive;
    private Boolean is2FAEnabled;
    private Boolean hasPassword;
    private String authProvider;
    private String googleId;
    private Long creditsBalance;
    private LocalDateTime createdAt;
}

