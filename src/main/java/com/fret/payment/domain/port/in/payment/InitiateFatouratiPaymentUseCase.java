package com.fret.payment.domain.port.in.payment;

import com.fret.payment.domain.model.payment.FatouratiToken;

import java.util.List;

public interface InitiateFatouratiPaymentUseCase {
    FatouratiToken initiate(Long invoiceId);
    FatouratiToken initiateGroup(List<Long> invoiceIds);
}
