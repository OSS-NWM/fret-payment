package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiPaymentHistoryDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("tokenRef")
    private String tokenRef;

    @JsonProperty("previousStatus")
    private String previousStatus;

    @JsonProperty("newStatus")
    private String newStatus;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("actor")
    private String actor;

    @JsonProperty("channel")
    private String channel;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("occurredAt")
    private LocalDateTime occurredAt;
}
