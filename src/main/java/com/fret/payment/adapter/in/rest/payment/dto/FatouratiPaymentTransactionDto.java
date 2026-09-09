package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiPaymentTransactionDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("tokenRef")
    private String tokenRef;

    @JsonProperty("aggregatorCode")
    private String aggregatorCode;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("terminalId")
    private String terminalId;

    @JsonProperty("fatouratiTransactionNumber")
    private String fatouratiTransactionNumber;

    @JsonProperty("paymentSystemTransactionNumber")
    private String paymentSystemTransactionNumber;

    @JsonProperty("paymentMode")
    private String paymentMode;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("transactionDate")
    private LocalDateTime transactionDate;

    @JsonProperty("receiptNumber")
    private String receiptNumber;

    @JsonProperty("status")
    private String status;

    @JsonProperty("selectedItems")
    private List<SelectedItem> selectedItems;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SelectedItem {
        @JsonProperty("id") private String id;
        @JsonProperty("amount") private BigDecimal amount;
    }
}
