package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "FatouratiCancelRequest",
        description = "CMI cancel callback payload (per CMI Fatourati API v1.2.4 spec section 4.2.3). " +
                "Sent by CMI when a token must be cancelled (e.g. timeout for CASH payments). " +
                "The merchant must verify the x-signature header before processing."
)
public class FatouratiCancelRequestDto {

    @Schema(description = "Merchant identifier", example = "100024")
    @JsonProperty("merchantCode") private String merchantCode;

    @Schema(description = "Store identifier", example = "100030")
    @JsonProperty("store") private String store;

    @Schema(description = "Token reference to cancel", example = "1000300000071")
    @JsonProperty("tokenRef") private String tokenRef;

    @Schema(description = "Order/créance identifier (optional)", example = "AMI-202607000001")
    @JsonProperty("orderId") private String orderId;

    @Schema(description = "Amount paid (optional, decimal with 2 places)", example = "100.00")
    @JsonProperty("totalAmount") private BigDecimal totalAmount;

    @Schema(description = "ISO 4217 currency code (optional, default 504 = MAD)", example = "504")
    @JsonProperty("currency") private String currency;

    @Schema(description = "Date of the transaction to cancel", example = "2026-09-09T20:05:53")
    @JsonProperty("transactionDate") private String transactionDate;

    @Schema(description = "Fatourati transaction number (optional)", example = "100003791199")
    @JsonProperty("fatouratiTransactionNumber") private String fatouratiTransactionNumber;
}
