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
@Schema(description = "CMI Fatourati payment token — returned after successful token generation")
public class FatouratiTokenResponseDto {

    @Schema(description = "CMI token reference (e.g. 1000300000071)", example = "1000300000071")
    @JsonProperty("tokenRef") private String tokenRef;

    @Schema(description = "Invoice ID this payment is for", example = "1001")
    @JsonProperty("invoiceId")
    private Long invoiceId;

    @Schema(description = "Mouvement ID this payment is for", example = "AMI-202607000001")
    @JsonProperty("mouvementId") private String mouvementId;

    @Schema(description = "Order ID (same as mouvementId)", example = "AMI-202607000001")
    @JsonProperty("orderId") private String orderId;

    @Schema(description = "Payment amount in MAD", example = "5000.00")
    @JsonProperty("totalAmount") private BigDecimal totalAmount;

    @Schema(description = "Currency code (504 = MAD)", example = "504")
    @JsonProperty("currency") private String currency;

    @Schema(description = "Base64 QR code PNG for mobile payment", example = "data:image/png;base64,...")
    @JsonProperty("qrCode") private String qrCode;

    @Schema(description = "Available payment channels", example = "[\"CFGMOBILE.t\", \"CDMBanqueDirect\"]")
    @JsonProperty("channels") private List<String> channels;

    @Schema(description = "Token expiry datetime", example = "2026-09-07T17:25:55")
    @JsonProperty("expiresAt") private LocalDateTime expiresAt;

    @Schema(description = "Token status", example = "CREATED")
    @JsonProperty("status") private String status;
}
