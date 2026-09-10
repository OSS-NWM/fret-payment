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
    private Long invoiceId;
    private String mouvementId;
    private BigDecimal totalAmount;
    private String currency;
    private String transactionNumber;
    private String transactionDate;
    private String channel;
    private String operator;
    private String aggregatorCode;
    private String paymentSystemTransactionNumber;
}
