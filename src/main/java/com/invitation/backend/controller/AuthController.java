package com.invitation.backend.controller;

import com.invitation.backend.dto.ApiResponse;
import com.invitation.backend.dto.request.*;
import com.invitation.backend.dto.response.*;
import com.invitation.backend.entity.User;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.security.UserDetailsImpl;
import com.invitation.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.ok("Google login successful", response));
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<UserDto>> setPassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody SetPasswordRequest request) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserDto updated = authService.setPassword(user, request);
        return ResponseEntity.ok(ApiResponse.ok("Password set successfully", updated));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<ApiResponse<AuthResponse>> verify2FA(@Valid @RequestBody Verify2FARequest request) {
        AuthResponse response = authService.verify2FA(request);
        return ResponseEntity.ok(ApiResponse.ok("2FA verified successfully", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized"));
        }
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return ResponseEntity.ok(ApiResponse.ok(authService.getCurrentUserDto(user)));
    }

    @PostMapping("/2fa/setup")
    public ResponseEntity<ApiResponse<Setup2FAResponse>> setup2FA(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Setup2FAResponse response = authService.setup2FA(user);
        return ResponseEntity.ok(ApiResponse.ok("2FA setup initialized", response));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<ApiResponse<Boolean>> enable2FA(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody Enable2FARequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        boolean enabled = authService.enable2FA(user, request.getCode());
        return ResponseEntity.ok(ApiResponse.ok("Two-Factor Authentication enabled successfully", enabled));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<ApiResponse<Boolean>> disable2FA(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody Map<String, String> body) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        String password = body.getOrDefault("password", "");
        boolean disabled = authService.disable2FA(user, password);
        return ResponseEntity.ok(ApiResponse.ok("Two-Factor Authentication disabled successfully", disabled));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        authService.changePassword(user, request);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", null));
    }
}

