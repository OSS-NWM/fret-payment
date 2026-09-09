package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "FatouratiPaymentTransaction",
        description = "Full CMI transaction record captured on payment callback. One transaction per CMI callback " +
                "(may include retries/duplicates with different fatouratiTransactionNumber values). " +
                "Mirrors the CMI Fatourati API v1.2.4 spec section 4.2.1."
)
public class FatouratiPaymentTransactionDto {

    @Schema(description = "Internal numeric ID of this transaction record", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Fatourati token reference this transaction applies to", example = "1000300000194")
    @JsonProperty("tokenRef")
    private String tokenRef;

    @Schema(description = "Aggregator code (when the merchant uses an aggregator intermediary)", example = "AGG001")
    @JsonProperty("aggregatorCode")
    private String aggregatorCode;

    @Schema(description = "Payment channel code used (2-character identifier)", example = "MOBILE_MONEY")
    @JsonProperty("channel")
    private String channel;

    @Schema(description = "Payment operator name (bank/wallet identifier)", example = "ORANGE_MAROC")
    @JsonProperty("operator")
    private String operator;

    @Schema(description = "Terminal identifier of the payment device", example = "1231542")
    @JsonProperty("terminalId")
    private String terminalId;

    @Schema(description = "Unique transaction identifier in the CMI platform", example = "100003791199")
    @JsonProperty("fatouratiTransactionNumber")
    private String fatouratiTransactionNumber;

    @Schema(description = "Transaction identifier at the payment operator level", example = "791199")
    @JsonProperty("paymentSystemTransactionNumber")
    private String paymentSystemTransactionNumber;

    @Schema(description = "Payment mode used (e.g. MULTI_CANAL, CASH, EXTERNAL)", example = "MULTI_CANAL")
    @JsonProperty("paymentMode")
    private String paymentMode;

    @Schema(description = "Amount paid in this transaction (decimal with 2 places)", example = "100.00")
    @JsonProperty("amount")
    private BigDecimal amount;

    @Schema(description = "ISO 4217 currency code (504 = MAD)", example = "504")
    @JsonProperty("currency")
    private String currency;

    @Schema(description = "Date/time when CMI processed the transaction", example = "2026-09-09T20:05:53")
    @JsonProperty("transactionDate")
    private LocalDateTime transactionDate;

    @Schema(description = "Receipt number we returned to CMI as our payment confirmation", example = "REC1725903952103ABCD")
    @JsonProperty("receiptNumber")
    private String receiptNumber;

    @Schema(description = "Transaction status as recorded by our system",
            example = "PAID",
            allowableValues = {"PAID", "REJECTED", "UNKNOWN"})
    @JsonProperty("status")
    private String status;

    @Schema(description = "Per-item breakdown of what was paid in this transaction",
            example = "[{\"id\": \"AMI-202607000050\", \"amount\": 100.00}]")
    @JsonProperty("selectedItems")
    private List<SelectedItem> selectedItems;

    @Schema(description = "Timestamp when this record was created in our database", example = "2026-09-09T20:05:54")
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Item paid in the transaction — id and amount")
    public static class SelectedItem {
        @Schema(description = "Item identifier (e.g. mouvement id or service id)", example = "AMI-202607000050")
        @JsonProperty("id") private String id;

        @Schema(description = "Amount of this item (decimal with 2 places)", example = "100.00")
        @JsonProperty("amount") private BigDecimal amount;
    }
}
