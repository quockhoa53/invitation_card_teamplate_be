package com.invitation.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED,
        CANCELLED,
        UNDERPAID,
        SETTLED_TO_WALLET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    @JsonIgnore
    private Card card;

    @Column(nullable = false, unique = true, length = 64)
    private String orderCode;

    @Column(nullable = false, length = 50)
    private String paymentMethod; // VIETQR, PAYOS, MOMO, WALLET

    @Column(nullable = false)
    private Long amount; // Số tiền nạp yêu cầu

    @Column(nullable = false)
    @Builder.Default
    private Long bonusAmount = 0L; // Tiền thưởng khuyến mãi (+10k, +30k)

    private Long actualAmount; // Số tiền thực tế nhận từ ngân hàng

    @Builder.Default
    private Long missingAmount = 0L; // Số tiền còn thiếu nếu chuyển thiếu

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "DEPOSIT"; // DEPOSIT, CARD_PURCHASE, WITHDRAWAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String gatewayPayload;

    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
