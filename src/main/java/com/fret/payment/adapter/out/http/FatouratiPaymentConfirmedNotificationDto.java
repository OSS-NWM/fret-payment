package com.fret.payment.adapter.out.http;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiPaymentConfirmedNotificationDto {
    private String tokenRef;
    private String mouvementId;
    private BigDecimal totalAmount;
    private String currency;
    private String transactionNumber;
    private String transactionDate;
}
