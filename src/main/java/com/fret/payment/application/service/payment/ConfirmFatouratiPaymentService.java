package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.cmi.CmiSignatureUtil;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiCallbackLogRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiPaymentCallback;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.port.in.payment.ConfirmFatouratiPaymentUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmFatouratiPaymentService implements ConfirmFatouratiPaymentUseCase {

    private final FatouratiTokenRepositoryAdapter tokenRepository;
    private final FatouratiCallbackLogRepositoryAdapter callbackLogRepository;
    private final CmiSignatureUtil signatureUtil;
    private final CmiProperties cmiProperties;
    private final ObjectMapper objectMapper;

    @Override
    public String confirmPayment(FatouratiPaymentCallback callback) {
        log.info("[FATOURATI_CONFIRM] Processing callback: tokenRef={}, decisionCode={}, numTrx={}",
                callback.getTokenRef(), callback.getDecisionCode(), callback.getFatouratiTransactionNumber());

        String rawBody = toJson(callback);

        boolean signatureValid = verifyCallbackSignature(callback);

        if (callbackLogRepository.isCallbackProcessed(callback.getFatouratiTransactionNumber())) {
            log.info("[FATOURATI_CONFIRM] Callback already processed: numTrx={}",
                    callback.getFatouratiTransactionNumber());
            callbackLogRepository.logCallback(
                    callback.getTokenRef(), callback.getPaymentSystemTransactionNumber(),
                    callback.getFatouratiTransactionNumber(), callback.getPaymentSystemTransactionNumber(),
                    callback.getTotalAmount(), rawBody, signatureValid,
                    callback.getDecisionCode(), "DUPLICATE_CALLBACK");
            return "2";
        }

        callbackLogRepository.logCallback(
                callback.getTokenRef(), callback.getPaymentSystemTransactionNumber(),
                callback.getFatouratiTransactionNumber(), callback.getPaymentSystemTransactionNumber(),
                callback.getTotalAmount(), rawBody, signatureValid,
                callback.getDecisionCode(), null);

        if (!signatureValid) {
            log.warn("[FATOURATI_CONFIRM] Invalid signature for tokenRef={}", callback.getTokenRef());
            return "3";
        }

        var tokenOpt = tokenRepository.findByTokenRef(callback.getTokenRef());
        if (tokenOpt.isEmpty()) {
            log.warn("[FATOURATI_CONFIRM] Token not found: tokenRef={}", callback.getTokenRef());
            return "3";
        }

        var token = tokenOpt.get();

        if (callback.getDecisionCode() == null || callback.getDecisionCode() == 0) {
            tokenRepository.updateStatus(callback.getTokenRef(), FatouratiTokenStatus.CONSUMED);
            log.info("[FATOURATI_CONFIRM] Payment approved: tokenRef={}", callback.getTokenRef());
        } else if (callback.getDecisionCode() == 1) {
            log.info("[FATOURATI_CONFIRM] Payment refused: tokenRef={}", callback.getTokenRef());
        } else {
            log.warn("[FATOURATI_CONFIRM] Unknown decisionCode={} for tokenRef={}",
                    callback.getDecisionCode(), callback.getTokenRef());
        }

        return "0";
    }

    private boolean verifyCallbackSignature(FatouratiPaymentCallback callback) {
        if (callback.getSignature() == null || callback.getSignature().isBlank()) {
            log.warn("[FATOURATI_CONFIRM] No x-signature provided in callback");
            return false;
        }
        try {
            String totalAmount = callback.getTotalAmount() != null
                    ? signatureUtil.formatAmount(callback.getTotalAmount()) : "";

            String signatureData = signatureUtil.buildCallbackSignatureData(
                    totalAmount,
                    callback.getCurrency() != null ? callback.getCurrency() : "",
                    cmiProperties.getMerchantCode() != null ? cmiProperties.getMerchantCode() : "",
                    cmiProperties.getStore() != null ? cmiProperties.getStore() : "",
                    callback.getOperator() != null ? callback.getOperator() : "",
                    callback.getChannel() != null ? callback.getChannel() : "",
                    callback.getTokenRef() != null ? callback.getTokenRef() : "",
                    callback.getOrderId() != null ? callback.getOrderId() : "",
                    callback.getPaymentMode() != null ? callback.getPaymentMode() : "",
                    callback.getFatouratiTransactionNumber() != null ? callback.getFatouratiTransactionNumber() : "",
                    cmiProperties.getStoreApiKey() != null ? cmiProperties.getStoreApiKey() : ""
            );

            String expected = signatureUtil.computeSignature(signatureData, cmiProperties.getStoreApiKey());
            boolean valid = signatureUtil.constantTimeEquals(expected, callback.getSignature());
            if (!valid) {
                log.warn("[FATOURATI_CONFIRM] Signature mismatch for tokenRef={}: expected={}, got={}",
                        callback.getTokenRef(), expected.substring(0, 8), callback.getSignature().substring(0, 8));
            }
            return valid;
        } catch (Exception e) {
            log.error("[FATOURATI_CONFIRM] Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    public String generateReceiptNumber() {
        return "REC" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
