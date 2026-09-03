package com.invitation.backend.service;

public interface QrCodeGeneratorService {
    String generateQrCodeBase64(String content, int width, int height);
}
