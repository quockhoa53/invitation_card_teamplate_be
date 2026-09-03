package com.invitation.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_2fa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User2FA {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @JsonIgnore
    private User user;

    @Column(nullable = true, columnDefinition = "TEXT")
    @JsonIgnore
    private String encryptedSecretKey;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Builder.Default
    @Column(length = 20)
    private String twoFactorType = "EMAIL"; // "EMAIL" or "TOTP"

    @Column(length = 10)
    @JsonIgnore
    private String emailOtpCode;

    private LocalDateTime emailOtpExpiresAt;

    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String backupCodes; // Comma-separated hashed backup codes

    private LocalDateTime enabledAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
