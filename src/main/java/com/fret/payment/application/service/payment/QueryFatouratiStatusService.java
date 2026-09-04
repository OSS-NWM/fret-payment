package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiFatouratiClientAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;
import com.fret.payment.domain.port.in.payment.GetFatouratiTransactionStatusUseCase;
import com.fret.payment.domain.port.in.payment.GetFatouratiTokenStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueryFatouratiStatusService implements GetFatouratiTransactionStatusUseCase, GetFatouratiTokenStatusUseCase {

    private final FatouratiTokenRepositoryAdapter tokenRepository;
    private final CmiFatouratiClientAdapter cmiClient;

    @Override
    public FatouratiTransactionStatus getStatus(String mouvementId) {
        log.debug("[FATOURATI_STATUS] Querying status for mouvementId={}", mouvementId);

        Optional<FatouratiToken> tokenOpt = tokenRepository.findActiveByMouvementId(mouvementId);
        if (tokenOpt.isEmpty()) {
            return FatouratiTransactionStatus.NOT_FOUND;
        }

        var token = tokenOpt.get();
        if (token.getStatus() == FatouratiTokenStatus.CANCELLED) {
            return FatouratiTransactionStatus.CANCELLED;
        }
        if (token.getStatus() == FatouratiTokenStatus.CONSUMED) {
            return FatouratiTransactionStatus.PAID;
        }

        if (token.getExpiresAt() != null && LocalDateTime.now().isAfter(token.getExpiresAt())) {
            return FatouratiTransactionStatus.NOT_FOUND;
        }

        return FatouratiTransactionStatus.PENDING;
    }

    @Override
    public FatouratiToken getToken(String mouvementId) {
        return tokenRepository.findActiveByMouvementId(mouvementId).orElse(null);
    }
}
