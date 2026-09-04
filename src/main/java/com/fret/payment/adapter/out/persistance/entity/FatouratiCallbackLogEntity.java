package com.fret.payment.adapter.out.persistance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fatourati_callback_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiCallbackLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_ref", length = 64)
    private String tokenRef;

    @Column(name = "sys_pmt_code", length = 64)
    private String sysPmtCode;

    @Column(name = "num_trx_fatourati", length = 64)
    private String numTrxFatourati;

    @Column(name = "num_trx_sys_pmt", length = 64)
    private String numTrxSysPmt;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_body", columnDefinition = "jsonb")
    private String rawBody;

    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid;

    @Column(name = "decision_code")
    private Integer decisionCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @PrePersist
    protected void onCreate() {
        receivedAt = LocalDateTime.now();
    }
}
