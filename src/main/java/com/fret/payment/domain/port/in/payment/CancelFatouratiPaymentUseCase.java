package com.fret.payment.domain.port.in.payment;

public interface CancelFatouratiPaymentUseCase {
    void cancel(String mouvementId);
}
