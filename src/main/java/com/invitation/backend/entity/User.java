package com.invitation.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @Column(nullable = true)
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasPassword = true;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String authProvider = "LOCAL"; // "LOCAL", "GOOGLE"

    @Column(length = 150)
    private String googleId;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Long creditsBalance = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long realBalance = 0L; // Tiền nạp thật có thể rút

    @Column(nullable = false)
    @Builder.Default
    private Long bonusBalance = 0L; // Tiền thưởng khuyến mãi (chỉ dùng mua thiệp)

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User2FA twoFactorAuth;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
