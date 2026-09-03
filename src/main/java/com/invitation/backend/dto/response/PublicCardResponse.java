package com.invitation.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCardResponse {
    private UUID id;
    private TemplateResponse template;
    private String title;
    private String slug;

    @JsonProperty("isProtected")
    private Boolean isProtected; // If true, card requires passcode to reveal full customData

    private String customData; // Null or masked if protected and not yet verified
    private Boolean isPublished;
    private Long viewCount;
    private List<WishResponse> wishes;
    private LocalDateTime createdAt;
}

