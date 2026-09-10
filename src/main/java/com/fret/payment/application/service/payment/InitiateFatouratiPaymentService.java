package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiFatouratiClientAdapter;
import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.cmi.InvoiceLinePayload;
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
import java.util.ArrayList;
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
    public FatouratiToken initiate(Long invoiceId) {
        log.info("[FATOURATI_INIT] Initiating payment for invoiceId={}", invoiceId);

        var existing = tokenRepository.findActiveByInvoiceId(invoiceId);
        if (existing.isPresent()) {
            log.info("[FATOURATI_INIT] Active token exists for invoiceId={}, returning existing", invoiceId);
            return existing.get();
        }

        InvoiceInfo invoice = invoiceInfoPort.findById(invoiceId);
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }
        if (invoice.getLignes() == null || invoice.getLignes().isEmpty()) {
            throw new IllegalStateException("Invoice has no lines: " + invoiceId);
        }

        String orderId = invoice.getNumeroPiece() != null
                ? invoice.getNumeroPiece()
                : "INV-" + invoiceId;

        List<InvoiceLinePayload> linePayloads = invoice.getLignes().stream()
                .map(l -> InvoiceLinePayload.builder()
                        .idLine(l.getIdLine())
                        .codeArticle(l.getCodeArticle())
                        .description(l.getDesignation())
                        .amount(l.getMontantHt() != null ? l.getMontantHt() : BigDecimal.ZERO)
                        .build())
                .toList();

        BigDecimal totalAmount = linePayloads.stream()
                .map(InvoiceLinePayload::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[FATOURATI_INIT] Invoice has zero total amount for invoiceId={}, using default 100.00", invoiceId);
            totalAmount = BigDecimal.valueOf(100.00);
        }

        String currency = invoice.getDevise() != null ? invoice.getDevise() : "504";
        String callbackUrl = cmiProperties.getCallbackUrl();
        String cancelUrl = cmiProperties.getCancelUrl();
        String checkStatusUrl = cmiProperties.getCheckStatusUrl();

        FatouratiToken token = cmiClient.generateToken(
                invoiceId,
                orderId,
                linePayloads,
                totalAmount,
                currency,
                callbackUrl,
                cancelUrl,
                checkStatusUrl
        );

        token.setInvoiceId(invoiceId);
        token.setMouvementId(invoice.getMouvementId());
        token.setInvoiceLineIds(linePayloads.stream().map(InvoiceLinePayload::getIdLine).toList());
        token.setTotalAmount(totalAmount);
        token.setStatus(FatouratiTokenStatus.CREATED);
        token = tokenRepository.save(token);

        log.info("[FATOURATI_INIT] Token created: tokenRef={}, invoiceId={}, amount={}",
                token.getTokenRef(), invoiceId, totalAmount);
        return token;
    }

    @Override
    public FatouratiToken initiateGroup(List<Long> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new IllegalArgumentException("invoiceIds cannot be empty");
        }

        log.info("[FATOURATI_INIT] Initiating group payment for invoiceIds={}", invoiceIds);

        List<InvoiceLinePayload> allItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        String primaryMouvementId = null;
        Long primaryInvoiceId = invoiceIds.get(0);

        for (Long invoiceId : invoiceIds) {
            InvoiceInfo invoice = invoiceInfoPort.findById(invoiceId);
            if (invoice == null) {
                throw new IllegalArgumentException("Invoice not found: " + invoiceId);
            }
            if (primaryMouvementId == null) {
                primaryMouvementId = invoice.getMouvementId();
            }
            if (invoice.getLignes() != null) {
                for (var line : invoice.getLignes()) {
                    BigDecimal lineAmount = line.getMontantHt() != null ? line.getMontantHt() : BigDecimal.ZERO;
                    allItems.add(InvoiceLinePayload.builder()
                            .idLine(line.getIdLine())
                            .codeArticle(line.getCodeArticle())
                            .description(line.getDesignation())
                            .amount(lineAmount)
                            .build());
                    totalAmount = totalAmount.add(lineAmount);
                }
            }
        }

        if (allItems.isEmpty()) {
            throw new IllegalStateException("No invoice lines found for invoiceIds: " + invoiceIds);
        }

        String orderId = "GROUP-" + String.join("-", invoiceIds.stream().map(String::valueOf).toList());
        String currency = "504";
        String callbackUrl = cmiProperties.getCallbackUrl();
        String cancelUrl = cmiProperties.getCancelUrl();
        String checkStatusUrl = cmiProperties.getCheckStatusUrl();

        FatouratiToken token = cmiClient.generateToken(
                primaryInvoiceId,
                orderId,
                allItems,
                totalAmount,
                currency,
                callbackUrl,
                cancelUrl,
                checkStatusUrl
        );

        token.setInvoiceId(primaryInvoiceId);
        token.setMouvementId(primaryMouvementId);
        token.setInvoiceLineIds(allItems.stream().map(InvoiceLinePayload::getIdLine).toList());
        token.setTotalAmount(totalAmount);
        token.setStatus(FatouratiTokenStatus.CREATED);
        token = tokenRepository.save(token);

        log.info("[FATOURATI_INIT] Group token created: tokenRef={}, invoiceIds={}, amount={}",
                token.getTokenRef(), invoiceIds, totalAmount);
        return token;
    }
}
