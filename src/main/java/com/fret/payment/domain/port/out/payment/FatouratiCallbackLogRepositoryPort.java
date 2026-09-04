package com.fret.payment.domain.port.out.payment;

import java.math.BigDecimal;

public interface FatouratiCallbackLogRepositoryPort {

    void logCallback(String tokenRef, String sysPmtCode, String numTrxFatourati,
                     String numTrxSysPmt, BigDecimal totalAmount,
                     String rawBody, boolean signatureValid,
                     Integer decisionCode, String errorMessage);

    boolean isCallbackProcessed(String numTrxFatourati);
}
