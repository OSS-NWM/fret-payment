package com.fret.payment.domain.model.payment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceInfo {

    private Long id;
    private String numeroPiece;
    private String mouvementId;
    private BigDecimal montantTtc;
    private String devise;
    private String typePrestation;
    private String statutPaiement;
    private List<InvoiceLineInfo> lignes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoiceLineInfo {
        private Long idLine;
        private String codeArticle;
        private String designation;
        private String unite;
        private Integer qte;
        private BigDecimal prixUnitaire;
        private BigDecimal remise;
        private BigDecimal montantHt;
        private BigDecimal montantHtMad;
        private LocalDateTime dateOperation;
    }
}
