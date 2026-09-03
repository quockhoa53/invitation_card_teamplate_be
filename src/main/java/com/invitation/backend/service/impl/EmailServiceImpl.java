package com.invitation.backend.service.impl;

import com.invitation.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${app.brevo.api-key:${BREVO_API_KEY:}}")
    private String brevoApiKey;

    @Value("${app.resend.api-key:${RESEND_API_KEY:}}")
    private String resendApiKey;

    @Value("${app.resend.from-email:KD Card <onboarding@resend.dev>}")
    private String fromEmail;

    @Value("${app.resend.test-fallback-email:${NOTIFY_EMAIL:nguyenquockhoa5549@gmail.com}}")
    private String fallbackEmail;

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, String purpose) {
        log.info("📧 [OTP DISPATCH] Destination: {}, Purpose: {}, OTP Code: {}", toEmail, purpose, otpCode);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            boolean sent = false;

            // 1. Try Brevo REST API (HTTPS 443) if API Key is configured
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                try {
                    sendViaBrevoApi(toEmail, otpCode, purpose);
                    sent = true;
                } catch (Exception e) {
                    log.warn("⚠️ Gửi qua Brevo REST API chưa thành công: {}", e.getMessage());
                }
            }

            // 2. Try Brevo SMTP / JavaMailSender (smtp-relay.brevo.com:587)
            if (!sent && mailSender != null) {
                try {
                    MimeMessage message = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                    helper.setFrom("nguyenquockhoa5549@gmail.com", "KD Card Security");
                    helper.setTo(toEmail);
                    helper.setSubject("🔒 [" + otpCode + "] Mã Xác Thực Đăng Nhập KD Card (Hạn dùng 5 phút)");

                    String htmlContent = buildOtpHtmlTemplate(otpCode, purpose);
                    helper.setText(htmlContent, true);

                    mailSender.send(message);
                    log.info("✅ Email OTP đã được gửi thành công qua Brevo SMTP đến {}", toEmail);
                    sent = true;
                } catch (Exception e) {
                    log.warn("⚠️ Gửi qua SMTP đến {} thất bại (Có thể cổng 587 bị Render chặn): {}", toEmail, e.getMessage());
                }
            }

            // 3. Try Resend HTTP API (Port 443 HTTPS)
            if (!sent && resendApiKey != null && !resendApiKey.isBlank()) {
                try {
                    sendViaResend(toEmail, otpCode, purpose, null);
                    sent = true;
                } catch (Exception e) {
                    log.warn("⚠️ Gửi qua Resend đến {} chưa thành công: {}", toEmail, e.getMessage());

                    // Sandbox Fallback
                    if (fallbackEmail != null && !fallbackEmail.isBlank() && !fallbackEmail.equalsIgnoreCase(toEmail)) {
                        try {
                            log.info("🔄 [SANDBOX FORWARD] Gửi mã OTP về Gmail chính chủ {} cho tài khoản {}", fallbackEmail, toEmail);
                            sendViaResend(fallbackEmail, otpCode, purpose, toEmail);
                            sent = true;
                        } catch (Exception ex) {
                            log.error("❌ Sandbox forward cũng thất bại: {}", ex.getMessage());
                        }
                    }
                }
            }

            if (!sent) {
                log.error("❌ Không thể gửi email qua các kênh. Mã OTP dùng khẩn cấp để đăng nhập: {}", otpCode);
            }
        });
    }

    private void sendViaBrevoApi(String toEmail, String otpCode, String purpose) {
        org.springframework.web.client.RestClient restClient = org.springframework.web.client.RestClient.builder()
                .baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", brevoApiKey.trim())
                .defaultHeader("Content-Type", org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept", org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .build();

        String subject = "🔒 [" + otpCode + "] Mã Xác Thực Đăng Nhập KD Card (Hạn dùng 5 phút)";
        String htmlContent = buildOtpHtmlTemplate(otpCode, purpose);

        java.util.Map<String, Object> payload = java.util.Map.of(
                "sender", java.util.Map.of("name", "KD Card Security", "email", "nguyenquockhoa5549@gmail.com"),
                "to", java.util.List.of(java.util.Map.of("email", toEmail)),
                "subject", subject,
                "htmlContent", htmlContent
        );

        String response = restClient.post()
                .uri("/smtp/email")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        log.info("✅ Brevo REST API đã gửi mail thành công đến {}: {}", toEmail, response);
    }

    private void sendViaResend(String toEmail, String otpCode, String purpose, String originalAccount) {
        org.springframework.web.client.RestClient restClient = org.springframework.web.client.RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + resendApiKey.trim())
                .defaultHeader("Content-Type", org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
                .build();

        String subject = originalAccount == null
                ? "🔒 [" + otpCode + "] Mã Xác Thực Đăng Nhập KD Card (Hạn dùng 5 phút)"
                : "🔒 [" + otpCode + "] [TEST] Mã OTP Cho Tài Khoản: " + originalAccount;

        String htmlContent = buildOtpHtmlTemplate(otpCode, originalAccount == null ? purpose : purpose + " (Tài khoản: " + originalAccount + ")");

        java.util.Map<String, Object> payload = java.util.Map.of(
                "from", fromEmail,
                "to", java.util.List.of(toEmail),
                "subject", subject,
                "html", htmlContent
        );

        String response = restClient.post()
                .uri("/emails")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(String.class);

        log.info("✅ Resend API đã gửi mail thành công đến {}: {}", toEmail, response);
    }

    private String buildOtpHtmlTemplate(String otpCode, String purpose) {
        return """
            <!DOCTYPE html>
            <html lang="vi">
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #0f172a; margin: 0; padding: 20px; color: #f8fafc; }
                    .container { max-width: 520px; margin: 0 auto; background: #1e293b; border-radius: 20px; border: 1px solid #334155; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.3); }
                    .header { background: linear-gradient(135deg, #e11d48, #f43f5e, #f59e0b); padding: 30px 20px; text-align: center; }
                    .brand { font-size: 24px; font-weight: 800; color: #ffffff; letter-spacing: 1px; margin: 0; }
                    .subtitle { font-size: 13px; color: rgba(255,255,255,0.85); margin-top: 5px; }
                    .content { padding: 30px 25px; text-align: center; }
                    .purpose-text { font-size: 15px; color: #cbd5e1; margin-bottom: 25px; line-height: 1.5; }
                    .otp-box { background: #0f172a; border: 2px dashed #f43f5e; border-radius: 16px; padding: 18px 25px; display: inline-block; margin: 10px auto 25px auto; }
                    .otp-code { font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #fb7185; font-family: monospace; }
                    .warning { font-size: 12px; color: #94a3b8; line-height: 1.6; border-top: 1px solid #334155; padding-top: 20px; margin-top: 20px; }
                    .footer { padding: 15px 25px 25px 25px; text-align: center; font-size: 11px; color: #64748b; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1 class="brand">KD CARD ATELIER</h1>
                        <div class="subtitle">Bảo Mật Tài Khoản 2 Lớp</div>
                    </div>
                    <div class="content">
                        <p class="purpose-text">
                            Xin chào,<br>
                            Bạn vừa yêu cầu mã xác thực cho thao tác: <strong>%s</strong>.
                        </p>
                        <div class="otp-box">
                            <span class="otp-code">%s</span>
                        </div>
                        <div class="warning">
                            ⏱️ Mã xác thực này có hiệu lực trong vòng <strong>5 phút</strong>.<br>
                            ⚠️ Tuyệt đối <strong>không chia sẻ mã này</strong> cho bất kỳ ai, kể cả nhân viên hỗ trợ KD Card.
                        </div>
                    </div>
                    <div class="footer">
                        © 2026 KD Card Interactive Atelier. Mọi quyền được bảo lưu.
                    </div>
                </div>
            </body>
            </html>
            """.formatted(purpose, otpCode);
    }
}
