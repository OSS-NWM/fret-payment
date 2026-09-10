package com.fret.payment.adapter.out.persistance.entity;

import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "fatourati_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_ref", nullable = false, unique = true, length = 64)
    private String tokenRef;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "mouvement_id", length = 100)
    private String mouvementId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "invoice_line_ids", columnDefinition = "jsonb")
    private List<Long> invoiceLineIds;

    @Column(name = "order_id", length = 64)
    private String orderId;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", length = 8)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private FatouratiTokenStatus status;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channels", columnDefinition = "jsonb")
    private List<String> channels;

    @Column(name = "payment_channel", length = 64)
    private String paymentChannel;

    @Column(name = "payment_operator", length = 64)
    private String paymentOperator;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
