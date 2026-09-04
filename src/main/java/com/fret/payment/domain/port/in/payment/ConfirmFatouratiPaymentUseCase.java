package com.fret.payment.domain.port.in.payment;

import com.fret.payment.domain.model.payment.FatouratiPaymentCallback;

public interface ConfirmFatouratiPaymentUseCase {
    String confirmPayment(FatouratiPaymentCallback callback);
}
