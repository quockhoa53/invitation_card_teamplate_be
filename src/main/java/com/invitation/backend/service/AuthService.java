package com.invitation.backend.service;

import com.invitation.backend.dto.request.*;
import com.invitation.backend.dto.response.AuthResponse;
import com.invitation.backend.dto.response.Setup2FAResponse;
import com.invitation.backend.dto.response.UserDto;
import com.invitation.backend.entity.User;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse loginWithGoogle(GoogleLoginRequest request);
    AuthResponse verify2FA(Verify2FARequest request);
    Setup2FAResponse setup2FA(User user);
    boolean enable2FA(User user, String code);
    boolean disable2FA(User user, String password);
    void changePassword(User user, ChangePasswordRequest request);
    UserDto setPassword(User user, SetPasswordRequest request);
    UserDto getCurrentUserDto(User user);
    UserDto mapToDto(User user, boolean is2FAEnabled);
    void sendEmailOtpForSetup(User user);
    void resendEmailOtp(String tempToken);
}
