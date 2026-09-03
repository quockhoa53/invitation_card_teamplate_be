package com.invitation.backend.service.impl;

import com.invitation.backend.dto.request.*;
import com.invitation.backend.dto.response.AuthResponse;
import com.invitation.backend.dto.response.Setup2FAResponse;
import com.invitation.backend.dto.response.UserDto;
import com.invitation.backend.entity.Role;
import com.invitation.backend.entity.User;
import com.invitation.backend.entity.User2FA;
import com.invitation.backend.repository.User2FARepository;
import com.invitation.backend.repository.UserRepository;
import com.invitation.backend.security.JwtUtils;
import com.invitation.backend.security.TotpService;
import com.invitation.backend.service.AuthService;
import com.invitation.backend.service.QrCodeGeneratorService;
import com.invitation.backend.util.AesEncryptionUtil;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final User2FARepository user2FARepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final TotpService totpService;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final QrCodeGeneratorService qrCodeGeneratorService;
    private final com.invitation.backend.service.EmailService emailService;

    @Value("${spring.application.name:InvitationCard}")
    private String appName;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.ROLE_USER)
                .isActive(true)
                .creditsBalance(100L) // Starting complimentary balance
                .hasPassword(true)
                .build();

        user = userRepository.save(user);

        String token = jwtUtils.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .require2FA(false)
                .user(mapToDto(user, false))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không chính xác"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new IllegalStateException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        if (Boolean.FALSE.equals(user.getHasPassword()) || user.getPassword() == null) {
            throw new BadCredentialsException("Tài khoản này được đăng ký qua Google và chưa thiết lập mật khẩu. Vui lòng chọn 'Đăng nhập với Google' hoặc thiết lập mật khẩu.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Email hoặc mật khẩu không chính xác");
        }

        Optional<User2FA> twoFactorOpt = user2FARepository.findByUser(user);
        boolean is2FAEnabled = twoFactorOpt.map(User2FA::getIsEnabled).orElse(false);

        if (is2FAEnabled || user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_SUPER_ADMIN) {
            if (is2FAEnabled) {
                // Generate 6-digit OTP and send email
                String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
                User2FA user2FA = twoFactorOpt.get();
                user2FA.setEmailOtpCode(otp);
                user2FA.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
                user2FARepository.save(user2FA);

                emailService.sendOtpEmail(user.getEmail(), otp, "Đăng nhập tài khoản KD Card");

                String tempToken = jwtUtils.generateStage1Token(user);
                return AuthResponse.builder()
                        .require2FA(true)
                        .tempToken(tempToken)
                        .user(mapToDto(user, true))
                        .build();
            }
        }

        String token = jwtUtils.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .require2FA(false)
                .user(mapToDto(user, is2FAEnabled))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);

        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
            if (!Boolean.TRUE.equals(user.getIsActive())) {
                throw new IllegalStateException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
            }
            if (request.getAvatarUrl() != null && !request.getAvatarUrl().isBlank()) {
                user.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getFullName() != null && !request.getFullName().isBlank() && (user.getFullName() == null || user.getFullName().isBlank())) {
                user.setFullName(request.getFullName());
            }
            user = userRepository.save(user);
        } else {
            user = User.builder()
                    .email(email)
                    .fullName(request.getFullName() != null ? request.getFullName() : email.split("@")[0])
                    .avatarUrl(request.getAvatarUrl())
                    .role(Role.ROLE_USER)
                    .isActive(true)
                    .hasPassword(false)
                    .creditsBalance(100L)
                    .authProvider("GOOGLE")
                    .googleId(request.getGoogleId())
                    .build();
            user = userRepository.save(user);
        }

        Optional<User2FA> twoFactorOpt = user2FARepository.findByUser(user);
        boolean is2FAEnabled = twoFactorOpt.map(User2FA::getIsEnabled).orElse(false);

        if (is2FAEnabled) {
            String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
            User2FA user2FA = twoFactorOpt.get();
            user2FA.setEmailOtpCode(otp);
            user2FA.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            user2FARepository.save(user2FA);

            emailService.sendOtpEmail(user.getEmail(), otp, "Đăng nhập tài khoản KD Card qua Google");

            String tempToken = jwtUtils.generateStage1Token(user);
            return AuthResponse.builder()
                    .require2FA(true)
                    .tempToken(tempToken)
                    .user(mapToDto(user, true))
                    .build();
        }

        String token = jwtUtils.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .require2FA(false)
                .user(mapToDto(user, is2FAEnabled))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verify2FA(Verify2FARequest request) {
        if (!jwtUtils.isStage1Token(request.getTempToken())) {
            throw new BadCredentialsException("Phiên xác thực 2FA đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại.");
        }
        String email = jwtUtils.extractUsername(request.getTempToken());
        if (email == null) {
            throw new BadCredentialsException("Phiên xác thực 2FA đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));

        User2FA user2FA = user2FARepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Chưa thiết lập xác thực 2 bước cho tài khoản này"));

        boolean isValid = false;
        // 1. Verify via Email OTP
        if (user2FA.getEmailOtpCode() != null && user2FA.getEmailOtpExpiresAt() != null) {
            if (user2FA.getEmailOtpExpiresAt().isAfter(LocalDateTime.now()) &&
                    request.getCode().trim().equals(user2FA.getEmailOtpCode().trim())) {
                isValid = true;
                user2FA.setEmailOtpCode(null);
                user2FA.setEmailOtpExpiresAt(null);
                user2FARepository.save(user2FA);
            }
        }

        // 2. Fallback to TOTP if encryptedSecretKey is present
        if (!isValid && user2FA.getEncryptedSecretKey() != null) {
            try {
                String rawSecret = aesEncryptionUtil.decrypt(user2FA.getEncryptedSecretKey());
                int code = Integer.parseInt(request.getCode().trim());
                if (totpService.verifyCode(rawSecret, code)) {
                    isValid = true;
                }
            } catch (Exception ignored) {}
        }

        if (!isValid) {
            throw new BadCredentialsException("Mã xác thực OTP không chính xác hoặc đã hết hạn!");
        }

        String token = jwtUtils.generateToken(user);
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .require2FA(false)
                .user(mapToDto(user, true))
                .build();
    }

    @Override
    @Transactional
    public Setup2FAResponse setup2FA(User user) {
        // Send email OTP so user can confirm easily via Email
        sendEmailOtpForSetup(user);

        GoogleAuthenticatorKey credentials = totpService.createCredentials();
        String secretKey = credentials.getKey();
        String encrypted = aesEncryptionUtil.encrypt(secretKey);

        User2FA user2FA = user2FARepository.findByUser(user).orElseGet(() -> User2FA.builder()
                .user(user)
                .isEnabled(false)
                .build());

        user2FA.setEncryptedSecretKey(encrypted);
        user2FARepository.save(user2FA);

        String otpAuthUrl = totpService.getOtpAuthUrl(appName, user.getEmail(), credentials);
        String qrCodeBase64 = qrCodeGeneratorService.generateQrCodeBase64(otpAuthUrl, 250, 250);

        return Setup2FAResponse.builder()
                .secretKey(secretKey)
                .qrCodeUri(otpAuthUrl)
                .qrCodeBase64(qrCodeBase64)
                .build();
    }

    @Override
    @Transactional
    public boolean enable2FA(User user, String codeStr) {
        User2FA user2FA = user2FARepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Vui lòng khởi tạo thiết lập 2FA trước khi kích hoạt"));

        boolean isValid = false;
        // 1. Check Email OTP
        if (user2FA.getEmailOtpCode() != null && user2FA.getEmailOtpExpiresAt() != null) {
            if (user2FA.getEmailOtpExpiresAt().isAfter(LocalDateTime.now()) &&
                    codeStr.trim().equals(user2FA.getEmailOtpCode().trim())) {
                isValid = true;
                user2FA.setTwoFactorType("EMAIL");
            }
        }

        // 2. Fallback to TOTP
        if (!isValid && user2FA.getEncryptedSecretKey() != null) {
            try {
                String rawSecret = aesEncryptionUtil.decrypt(user2FA.getEncryptedSecretKey());
                int code = Integer.parseInt(codeStr.trim());
                if (totpService.verifyCode(rawSecret, code)) {
                    isValid = true;
                    user2FA.setTwoFactorType("TOTP");
                }
            } catch (Exception ignored) {}
        }

        if (!isValid) {
            throw new BadCredentialsException("Mã xác thực OTP không chính xác hoặc đã hết hạn!");
        }

        user2FA.setIsEnabled(true);
        user2FA.setEnabledAt(LocalDateTime.now());
        user2FA.setEmailOtpCode(null);
        user2FA.setEmailOtpExpiresAt(null);
        user2FARepository.save(user2FA);
        return true;
    }

    @Override
    @Transactional
    public void sendEmailOtpForSetup(User user) {
        User2FA user2FA = user2FARepository.findByUser(user).orElseGet(() -> User2FA.builder()
                .user(user)
                .isEnabled(false)
                .build());

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        user2FA.setEmailOtpCode(otp);
        user2FA.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        user2FARepository.save(user2FA);

        emailService.sendOtpEmail(user.getEmail(), otp, "Kích hoạt bảo mật 2 bước (2FA) qua Email");
    }

    @Override
    @Transactional
    public void resendEmailOtp(String tempToken) {
        if (!jwtUtils.isStage1Token(tempToken)) {
            throw new BadCredentialsException("Phiên xác thực đã hết hạn. Vui lòng đăng nhập lại.");
        }
        String email = jwtUtils.extractUsername(tempToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tồn tại"));

        User2FA user2FA = user2FARepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Chưa thiết lập xác thực 2 bước"));

        String otp = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        user2FA.setEmailOtpCode(otp);
        user2FA.setEmailOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        user2FARepository.save(user2FA);

        emailService.sendOtpEmail(user.getEmail(), otp, "Đăng nhập tài khoản KD Card");
    }

    @Override
    @Transactional
    public boolean disable2FA(User user, String password) {
        if (Boolean.TRUE.equals(user.getHasPassword()) && user.getPassword() != null) {
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new BadCredentialsException("Mật khẩu xác nhận không chính xác");
            }
        }

        User2FA user2FA = user2FARepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Tài khoản chưa bật 2FA"));

        user2FA.setIsEnabled(false);
        user2FARepository.save(user2FA);
        return true;
    }

    @Override
    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (Boolean.TRUE.equals(user.getHasPassword()) && user.getPassword() != null) {
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BadCredentialsException("Mật khẩu hiện tại không chính xác");
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setHasPassword(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserDto setPassword(User user, SetPasswordRequest request) {
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setHasPassword(true);
        user = userRepository.save(user);

        boolean is2FAEnabled = user2FARepository.findByUser(user)
                .map(User2FA::getIsEnabled)
                .orElse(false);

        return mapToDto(user, is2FAEnabled);
    }

    @Override
    public UserDto getCurrentUserDto(User user) {
        boolean is2FAEnabled = user2FARepository.findByUser(user)
                .map(User2FA::getIsEnabled)
                .orElse(false);
        return mapToDto(user, is2FAEnabled);
    }

    @Override
    public UserDto mapToDto(User user, boolean is2FAEnabled) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .creditsBalance(user.getCreditsBalance())
                .realBalance(user.getRealBalance() != null ? user.getRealBalance() : (user.getCreditsBalance() != null ? user.getCreditsBalance() : 0L))
                .bonusBalance(user.getBonusBalance() != null ? user.getBonusBalance() : 0L)
                .is2FAEnabled(is2FAEnabled)
                .hasPassword(Boolean.TRUE.equals(user.getHasPassword()))
                .authProvider(user.getAuthProvider())
                .googleId(user.getGoogleId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
