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
public class FatouratiPaymentCallback {

    private String merchantCode;
    private String store;
    private String tokenRef;
    private String orderId;
    private BigDecimal totalAmount;
    private String currency;
    private List<String> selectedItems;
    private LocalDateTime transactionDate;
    private String fatouratiTransactionNumber;
    private String paymentSystemTransactionNumber;
    private String paymentMode;
    private String channel;
    private String operator;
    private String extraData;
    private Integer decisionCode;
    private String signature;
}
