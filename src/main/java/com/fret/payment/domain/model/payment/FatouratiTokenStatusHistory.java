package com.fret.payment.domain.model.payment;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiTokenStatusHistory {

    private Long id;
    private String tokenRef;
    private FatouratiTokenStatus previousStatus;
    private FatouratiTokenStatus newStatus;
    private String reason;
    private String actor;
    private String channel;
    private String operator;
    private LocalDateTime occurredAt;
}
