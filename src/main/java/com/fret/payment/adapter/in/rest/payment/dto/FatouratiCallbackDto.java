package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiCallbackDto {

    @JsonProperty("merchantCode") private String merchantCode;
    @JsonProperty("store") private String store;
    @JsonProperty("tokenRef") private String tokenRef;
    @JsonProperty("orderId") private String orderId;
    @JsonProperty("totalAmount") private BigDecimal totalAmount;
    @JsonProperty("currency") private String currency;
    @JsonProperty("selectedItems") private List<String> selectedItems;
    @JsonProperty("transactionDate") private String transactionDate;
    @JsonProperty("fatouratiTransactionNumber") private String fatouratiTransactionNumber;
    @JsonProperty("paymentSystemTransactionNumber") private String paymentSystemTransactionNumber;
    @JsonProperty("paymentMode") private String paymentMode;
    @JsonProperty("channel") private String channel;
    @JsonProperty("operator") private String operator;
    @JsonProperty("extraData") private String extraData;
    @JsonProperty("decisionCode") private Integer decisionCode;
}
