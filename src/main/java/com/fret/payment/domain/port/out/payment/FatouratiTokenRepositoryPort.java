package com.fret.payment.domain.port.out.payment;

import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;

import java.util.Optional;

public interface FatouratiTokenRepositoryPort {

    FatouratiToken save(FatouratiToken token);

    Optional<FatouratiToken> findByTokenRef(String tokenRef);

    Optional<FatouratiToken> findByMouvementId(String mouvementId);

    void updateStatus(String tokenRef, FatouratiTokenStatus status);

    Optional<FatouratiToken> findActiveByMouvementId(String mouvementId);
}
