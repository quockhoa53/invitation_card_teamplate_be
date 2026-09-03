package com.invitation.backend.dto.response;

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
public class WishResponse {
    private UUID id;
    private String senderName;
    private String message;
    private String emotionIcon;
    private LocalDateTime createdAt;
}

