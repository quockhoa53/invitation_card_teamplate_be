package com.invitation.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Setup2FAResponse {
    private String secretKey;
    private String qrCodeUri;
    private String qrCodeBase64;
    private List<String> backupCodes;
}

