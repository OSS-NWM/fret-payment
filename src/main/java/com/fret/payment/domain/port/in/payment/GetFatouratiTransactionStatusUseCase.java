package com.fret.payment.domain.port.in.payment;

import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;

public interface GetFatouratiTransactionStatusUseCase {
    FatouratiTransactionStatus getStatus(String mouvementId);
}
