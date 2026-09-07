package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiFatouratiClientAdapter;
import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.InvoiceInfo;
import com.fret.payment.domain.port.in.payment.InitiateFatouratiPaymentUseCase;
import com.fret.payment.domain.port.out.payment.InvoiceInfoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitiateFatouratiPaymentService implements InitiateFatouratiPaymentUseCase {

    private final CmiFatouratiClientAdapter cmiClient;
    private final FatouratiTokenRepositoryAdapter tokenRepository;
    private final CmiProperties cmiProperties;
    private final InvoiceInfoPort invoiceInfoPort;

    @Override
    public FatouratiToken initiate(String mouvementId) {
        log.info("[FATOURATI_INIT] Initiating payment for mouvementId={}", mouvementId);

        var existing = tokenRepository.findActiveByMouvementId(mouvementId);
        if (existing.isPresent()) {
            log.info("[FATOURATI_INIT] Active token exists for mouvementId={}, returning existing", mouvementId);
            return existing.get();
        }

        BigDecimal amount = extractAmount(mouvementId);
        String currency = "504";
        String callbackUrl = cmiProperties.getCallbackUrl();
        String cancelUrl = cmiProperties.getCancelUrl();
        String checkStatusUrl = cmiProperties.getCheckStatusUrl();

        FatouratiToken token = cmiClient.generateToken(
                mouvementId,
                amount,
                currency,
                callbackUrl,
                cancelUrl,
                checkStatusUrl
        );

        token.setMouvementId(mouvementId);
        token.setTotalAmount(amount);
        token.setStatus(FatouratiTokenStatus.CREATED);
        token = tokenRepository.save(token);

        log.info("[FATOURATI_INIT] Token created: tokenRef={}, mouvementId={}, amount={}",
                token.getTokenRef(), mouvementId, amount);
        return token;
    }

    private BigDecimal extractAmount(String mouvementId) {
        List<InvoiceInfo> invoices = invoiceInfoPort.findByMouvementId(mouvementId);
        if (invoices == null || invoices.isEmpty()) {
            log.warn("[FATOURATI_INIT] No invoice found for mouvementId={}, using default amount", mouvementId);
            return BigDecimal.valueOf(100.00);
        }

        InvoiceInfo latestInvoice = invoices.get(invoices.size() - 1);
        BigDecimal montantTtc = latestInvoice.getMontantTtc();
        if (montantTtc == null || montantTtc.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[FATOURATI_INIT] Invoice has invalid montantTtc={} for mouvementId={}, using default",
                    montantTtc, mouvementId);
            return BigDecimal.valueOf(100.00);
        }

        return montantTtc;
    }
}
