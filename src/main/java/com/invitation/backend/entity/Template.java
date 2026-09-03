package com.invitation.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String category; // e.g. BIRTHDAY_LOVER, BIRTHDAY_FRIENDS, LOVE_ANNIVERSARY, EVENT_INVITATION

    @Column(length = 500)
    private String thumbnailUrl;

    @Column(length = 500)
    private String previewUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isFree = true;

    @Column(nullable = false)
    @Builder.Default
    private Long price = 0L; // Price in VND

    @Column(columnDefinition = "TEXT")
    private String defaultConfig; // JSON string for initial config (colors, music, texts)

    @Column(columnDefinition = "TEXT")
    private String schemaRules; // JSON string for customizable limits

    @Column(length = 30)
    @Builder.Default
    private String templateType = "BUILT_IN"; // "BUILT_IN" or "CUSTOM_CODE"

    @Column(columnDefinition = "TEXT")
    private String customHtml;

    @Column(columnDefinition = "TEXT")
    private String customCss;

    @Column(columnDefinition = "TEXT")
    private String customJs;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = false; // Draft by default until admin publishes

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
