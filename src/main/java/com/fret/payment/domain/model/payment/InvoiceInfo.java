package com.fret.payment.domain.model.payment;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceInfo {

    private String id;
    private String mouvementId;
    private BigDecimal montantTtc;
}
