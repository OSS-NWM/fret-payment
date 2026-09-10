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
@Schema(description = "Payment status response for an invoice or mouvement")
public class FatouratiStatusResponseDto {

    @Schema(description = "Invoice ID", example = "1001")
    @JsonProperty("invoiceId")
    private Long invoiceId;

    @Schema(description = "Mouvement ID", example = "AMI-202607000001")
    @JsonProperty("mouvementId") private String mouvementId;

    @Schema(description = "Payment status: PAID, CANCELLED, EXPIRED, PENDING, NOT_FOUND", example = "PAID")
    @JsonProperty("status") private String status;

    @Schema(description = "CMI token reference", example = "1000300000071")
    @JsonProperty("tokenRef") private String tokenRef;

    @Schema(description = "Payment amount in MAD", example = "5000.00")
    @JsonProperty("totalAmount") private BigDecimal totalAmount;

    @Schema(description = "Available payment channels for this token (CASH at bank agencies, electronic wallets, e-banking, etc.)",
            example = "[\"CFGMOBILE.t\", \"CDMBanqueDirect\", \"BMCI Connexis.t\", \"WafacashJibi\", \"CIHMOBILE.t\"]")
    @JsonProperty("channels") private List<String> channels;

    @Schema(description = "Token expiry datetime", example = "2026-09-07T17:25:55")
    @JsonProperty("expiresAt") private String expiresAt;
}
