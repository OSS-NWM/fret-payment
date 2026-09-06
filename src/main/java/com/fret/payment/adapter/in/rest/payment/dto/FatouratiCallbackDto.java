package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "CMI payment callback body — received from CMI on payment confirmation")
public class FatouratiCallbackDto {

    @Schema(example = "100024")
    @JsonProperty("merchantCode") private String merchantCode;

    @Schema(example = "100030")
    @JsonProperty("store") private String store;

    @Schema(example = "1000300000071")
    @JsonProperty("tokenRef") private String tokenRef;

    @Schema(example = "AMI-202607000001")
    @JsonProperty("orderId") private String orderId;

    @Schema(example = "5000.00")
    @JsonProperty("totalAmount") private BigDecimal totalAmount;

    @Schema(example = "504")
    @JsonProperty("currency") private String currency;

    @JsonProperty("selectedItems") private List<String> selectedItems;

    @Schema(example = "2026-09-06T15:30:00Z")
    @JsonProperty("transactionDate") private String transactionDate;

    @Schema(example = "FATO-2026-0001")
    @JsonProperty("fatouratiTransactionNumber") private String fatouratiTransactionNumber;

    @JsonProperty("paymentSystemTransactionNumber") private String paymentSystemTransactionNumber;

    @Schema(example = "MULTI_CANAL")
    @JsonProperty("paymentMode") private String paymentMode;

    @JsonProperty("channel") private String channel;

    @JsonProperty("operator") private String operator;

    @JsonProperty("extraData") private String extraData;

    @Schema(description = "Decision code: 0 = confirmed, 2 = already processed", example = "0")
    @JsonProperty("decisionCode") private Integer decisionCode;
}
