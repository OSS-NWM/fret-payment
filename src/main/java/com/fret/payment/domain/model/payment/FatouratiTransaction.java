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
public class FatouratiTransaction {

    private Long id;
    private String tokenRef;
    private String aggregatorCode;
    private String channel;
    private String operator;
    private String terminalId;
    private String fatouratiTransactionNumber;
    private String paymentSystemTransactionNumber;
    private String paymentMode;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime transactionDate;
    private String receiptNumber;
    private String status;
    private List<SelectedItem> selectedItems;
    private String rawPayload;
    private LocalDateTime createdAt;

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
