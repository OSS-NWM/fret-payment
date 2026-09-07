package com.fret.payment.adapter.out.persistance.entity;

import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fatourati_token_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiTokenStatusHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_ref", nullable = false, length = 64)
    private String tokenRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private FatouratiTokenStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private FatouratiTokenStatus newStatus;

    @Column(name = "reason", nullable = false, length = 64)
    private String reason;

    @Column(name = "actor", length = 64)
    private String actor;

    @Column(name = "channel", length = 64)
    private String channel;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt;

    @PrePersist
    protected void onCreate() {
        occurredAt = LocalDateTime.now();
    }
}
