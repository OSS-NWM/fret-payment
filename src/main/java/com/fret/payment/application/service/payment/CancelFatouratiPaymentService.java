package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.port.in.payment.CancelFatouratiPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelFatouratiPaymentService implements CancelFatouratiPaymentUseCase {

    private final FatouratiTokenRepositoryAdapter tokenRepository;

    @Override
    public void cancel(String mouvementId) {
        log.info("[FATOURATI_CANCEL] Cancelling payment for mouvementId={}", mouvementId);

        Optional<FatouratiToken> tokenOpt = tokenRepository.findActiveByMouvementId(mouvementId);
        if (tokenOpt.isEmpty()) {
            log.warn("[FATOURATI_CANCEL] No active token found for mouvementId={}", mouvementId);
            return;
        }

        var token = tokenOpt.get();
        tokenRepository.updateStatus(token.getTokenRef(), FatouratiTokenStatus.CANCELLED);
        log.info("[FATOURATI_CANCEL] Token cancelled locally: tokenRef={}, mouvementId={}",
                token.getTokenRef(), mouvementId);
    }

    public Optional<FatouratiToken> getTokenForStatus(String tokenRef) {
        return tokenRepository.findByTokenRef(tokenRef);
    }
}
