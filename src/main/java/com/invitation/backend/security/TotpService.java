package com.invitation.backend.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TotpService {

    private final GoogleAuthenticator gAuth;

    public TotpService() {
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(TimeUnit.SECONDS.toMillis(30))
                .setWindowSize(2) // Allow +/- 1 time step
                .build();
        this.gAuth = new GoogleAuthenticator(config);
    }

    public GoogleAuthenticatorKey createCredentials() {
        return gAuth.createCredentials();
    }

    public boolean verifyCode(String secretKey, int verificationCode) {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            return false;
        }
        return gAuth.authorize(secretKey, verificationCode);
    }

    public String getOtpAuthUrl(String issuer, String accountName, GoogleAuthenticatorKey credentials) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, accountName, credentials);
    }

    public List<String> generateBackupCodes(int count) {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < count; i++) {
            int code = 10000000 + random.nextInt(90000000);
            codes.add(String.valueOf(code));
        }
        return codes;
    }
}
