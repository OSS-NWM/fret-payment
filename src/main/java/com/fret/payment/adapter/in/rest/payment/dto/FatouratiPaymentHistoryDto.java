package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(
        name = "FatouratiPaymentHistoryEntry",
        description = "Single status transition entry in the Fatourati payment lifecycle audit trail. " +
                "Each transition (e.g. CREATED → CONSUMED, CREATED → CANCELLED) produces exactly one entry."
)
public class FatouratiPaymentHistoryDto {

    @Schema(description = "Internal numeric ID of this history entry", example = "3", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonProperty("id")
    private Long id;

    @Schema(description = "Fatourati token reference this entry applies to", example = "1000300000194")
    @JsonProperty("tokenRef")
    private String tokenRef;

    @Schema(description = "Previous status of the token before this transition. Null for the initial CREATED entry.", example = "CREATED", allowableValues = {"CREATED", "CONSUMED", "REJECTED", "EXPIRED", "CANCELLED"})
    @JsonProperty("previousStatus")
    private String previousStatus;

    @Schema(description = "New status of the token after this transition", example = "CONSUMED", allowableValues = {"CREATED", "CONSUMED", "REJECTED", "EXPIRED", "CANCELLED"})
    @JsonProperty("newStatus")
    private String newStatus;

    @Schema(description = "Reason for the transition", example = "PAYMENT_CONFIRMED",
            allowableValues = {"PAYMENT_CONFIRMED", "PAYMENT_REFUSED", "WEBHOOK_CANCEL", "ADMIN_CANCEL", "USER_CANCEL", "STATUS_UPDATE", "UNKNOWN_DECISION_CODE"})
    @JsonProperty("reason")
    private String reason;

    @Schema(description = "Actor that triggered the transition (CMI_WEBHOOK for inbound callbacks, JWT subject for admin actions)", example = "CMI_WEBHOOK")
    @JsonProperty("actor")
    private String actor;

    @Schema(description = "Payment channel used (populated only on PAYMENT_CONFIRMED transitions)", example = "MOBILE_MONEY")
    @JsonProperty("channel")
    private String channel;

    @Schema(description = "Payment operator (bank/wallet) used (populated only on PAYMENT_CONFIRMED transitions)", example = "ORANGE_MAROC")
    @JsonProperty("operator")
    private String operator;

    @Schema(description = "Timestamp when the transition was recorded (server local time)", example = "2026-09-09T18:53:52")
    @JsonProperty("occurredAt")
    private LocalDateTime occurredAt;
}
