package com.fret.payment.domain.port.out;

import java.math.BigDecimal;

public interface FretManagementNotifierPort {

    void notifyPaymentConfirmed(String tokenRef, String mouvementId, BigDecimal amount,
                                String currency, String transactionNumber);

    void notifyPaymentConfirmedByInvoiceId(String tokenRef, Long invoiceId, String mouvementId,
                                          BigDecimal amount, String currency, String transactionNumber,
                                          String channel, String operator, String aggregatorCode,
                                          String paymentSystemTransactionNumber);
}
