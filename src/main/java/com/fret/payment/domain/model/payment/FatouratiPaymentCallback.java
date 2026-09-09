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
    private String aggregatorCode;
    private String tokenRef;
    private String orderId;
    private BigDecimal totalAmount;
    private String currency;
    private List<SelectedItem> selectedItems;
    private LocalDateTime transactionDate;
    private String fatouratiTransactionNumber;
    private String paymentSystemTransactionNumber;
    private String paymentMode;
    private String channel;
    private String operator;
    private String terminalId;
    private Object extraData;
    private Integer decisionCode;
    private String signature;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedItem {
        private String id;
        private BigDecimal amount;
    }
}
