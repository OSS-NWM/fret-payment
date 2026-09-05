package com.fret.payment.domain.port.out;

import com.fret.payment.domain.model.payment.FatouratiPaymentCallback;

import java.math.BigDecimal;

public interface FretManagementNotifierPort {

    void notifyPaymentConfirmed(String tokenRef, String mouvementId, BigDecimal amount,
                                String currency, String transactionNumber);
}
