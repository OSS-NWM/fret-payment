package com.fret.payment.domain.model.payment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiToken {

    private Long id;
    private String tokenRef;
    private Long invoiceId;
    private String mouvementId;
    private List<Long> invoiceLineIds;
    private String orderId;
    private BigDecimal totalAmount;
    private String currency;
    private FatouratiTokenStatus status;
    private String qrCode;
    private List<String> channels;
    private String paymentChannel;
    private String paymentOperator;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
