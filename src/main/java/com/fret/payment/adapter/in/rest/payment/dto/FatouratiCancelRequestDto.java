package com.fret.payment.adapter.in.rest.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiCancelRequestDto {

    @JsonProperty("tokenRef") private String tokenRef;
    @JsonProperty("orderId") private String orderId;
}
