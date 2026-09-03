package com.invitation.backend.service;

public interface EmailService {
    void sendOtpEmail(String toEmail, String otpCode, String purpose);
}
