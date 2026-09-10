package com.fret.payment.adapter.out.cmi;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLinePayload {
    private Long idLine;
    private String codeArticle;
    private String description;
    private BigDecimal amount;
}
