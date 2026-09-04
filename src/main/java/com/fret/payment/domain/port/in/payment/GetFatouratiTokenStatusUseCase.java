package com.fret.payment.domain.port.in.payment;

import com.fret.payment.domain.model.payment.FatouratiToken;

public interface GetFatouratiTokenStatusUseCase {
    FatouratiToken getToken(String mouvementId);
}
