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
public class FatouratiStatusResponseDto {

    @JsonProperty("mouvementId") private String mouvementId;
    @JsonProperty("status") private String status;
    @JsonProperty("tokenRef") private String tokenRef;
    @JsonProperty("totalAmount") private BigDecimal totalAmount;
    @JsonProperty("channels") private List<String> channels;
    @JsonProperty("expiresAt") private String expiresAt;
}
