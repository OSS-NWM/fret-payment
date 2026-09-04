package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiTokenResponseDto {

    @JsonProperty("tokenRef") private String tokenRef;
    @JsonProperty("mouvementId") private String mouvementId;
    @JsonProperty("orderId") private String orderId;
    @JsonProperty("totalAmount") private BigDecimal totalAmount;
    @JsonProperty("currency") private String currency;
    @JsonProperty("qrCode") private String qrCode;
    @JsonProperty("channels") private List<String> channels;
    @JsonProperty("expiresAt") private LocalDateTime expiresAt;
    @JsonProperty("status") private String status;
}
