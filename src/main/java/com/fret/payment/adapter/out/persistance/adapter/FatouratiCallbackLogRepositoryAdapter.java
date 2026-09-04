package com.fret.payment.adapter.out.persistance.adapter;

import com.fret.payment.adapter.out.persistance.entity.FatouratiCallbackLogEntity;
import com.fret.payment.adapter.out.persistance.repository.FatouratiCallbackLogJpaRepository;
import com.fret.payment.domain.port.out.payment.FatouratiCallbackLogRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class FatouratiCallbackLogRepositoryAdapter implements FatouratiCallbackLogRepositoryPort {

    private final FatouratiCallbackLogJpaRepository jpaRepository;

    @Override
    public void logCallback(String tokenRef, String sysPmtCode, String numTrxFatourati,
                            String numTrxSysPmt, BigDecimal totalAmount,
                            String rawBody, boolean signatureValid,
                            Integer decisionCode, String errorMessage) {
        FatouratiCallbackLogEntity entity = FatouratiCallbackLogEntity.builder()
                .tokenRef(tokenRef)
                .sysPmtCode(sysPmtCode)
                .numTrxFatourati(numTrxFatourati)
                .numTrxSysPmt(numTrxSysPmt)
                .totalAmount(totalAmount)
                .rawBody(rawBody)
                .signatureValid(signatureValid)
                .decisionCode(decisionCode)
                .errorMessage(errorMessage)
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public boolean isCallbackProcessed(String numTrxFatourati) {
        return jpaRepository.existsByNumTrxFatourati(numTrxFatourati);
    }
}
