package com.fret.payment.adapter.in.rest.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to initiate a Fatourati payment for multiple invoices in a single token")
public class InitiateGroupPaymentRequest {

    @Schema(description = "List of invoice IDs to pay together", example = "[1001, 1002, 1003]")
    private List<Long> invoiceIds;
}
