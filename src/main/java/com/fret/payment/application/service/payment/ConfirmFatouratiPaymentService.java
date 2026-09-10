package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.cmi.CmiSignatureUtil;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiCallbackLogRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTransactionRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiPaymentCallback;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTransaction;
import com.fret.payment.domain.port.in.payment.ConfirmFatouratiPaymentUseCase;
import com.fret.payment.domain.port.out.FretManagementNotifierPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfirmFatouratiPaymentService implements ConfirmFatouratiPaymentUseCase {

    private final FatouratiTokenRepositoryAdapter tokenRepository;
    private final FatouratiCallbackLogRepositoryAdapter callbackLogRepository;
    private final FatouratiTransactionRepositoryAdapter transactionRepository;
    private final CmiSignatureUtil signatureUtil;
    private final CmiProperties cmiProperties;
    private final ObjectMapper objectMapper;
    private final FretManagementNotifierPort fretManagementNotifier;

    @Override
    @Transactional
    public String confirmPayment(FatouratiPaymentCallback callback) {
        log.info("[FATOURATI_CONFIRM] Processing callback: tokenRef={}, decisionCode={}, numTrx={}, channel={}, operator={}",
                callback.getTokenRef(), callback.getDecisionCode(), callback.getFatouratiTransactionNumber(),
                callback.getChannel(), callback.getOperator());

        String rawBody = toJson(callback);

        boolean signatureValid = verifyCallbackSignature(callback);

        if (!signatureValid) {
            log.warn("[FATOURATI_CONFIRM] Invalid signature for tokenRef={}", callback.getTokenRef());
            callbackLogRepository.logCallback(
                    callback.getTokenRef(), callback.getPaymentSystemTransactionNumber(),
                    callback.getFatouratiTransactionNumber(), callback.getPaymentSystemTransactionNumber(),
                    callback.getTotalAmount(), rawBody, signatureValid,
                    callback.getDecisionCode(), "INVALID_SIGNATURE");
            return "3";
        }

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

        var tokenOpt = tokenRepository.findByTokenRef(callback.getTokenRef());
        if (tokenOpt.isEmpty()) {
            log.warn("[FATOURATI_CONFIRM] Token not found: tokenRef={}", callback.getTokenRef());
            return "3";
        }

        var token = tokenOpt.get();
        String receiptNumber = generateReceiptNumber();

        if (callback.getDecisionCode() == null || callback.getDecisionCode() == 0) {
            tokenRepository.updateConfirmation(
                    callback.getTokenRef(),
                    FatouratiTokenStatus.CONSUMED,
                    callback.getChannel(),
                    callback.getOperator(),
                    "PAYMENT_CONFIRMED",
                    "CMI_WEBHOOK"
            );
            saveTransaction(callback, receiptNumber, "PAID");
            log.info("[FATOURATI_CONFIRM] Payment approved: tokenRef={}, channel={}, operator={}, receipt={}",
                    callback.getTokenRef(), callback.getChannel(), callback.getOperator(), receiptNumber);

            if (token.getInvoiceId() != null) {
                fretManagementNotifier.notifyPaymentConfirmedByInvoiceId(
                        callback.getTokenRef(),
                        token.getInvoiceId(),
                        token.getMouvementId(),
                        callback.getTotalAmount(),
                        callback.getCurrency(),
                        callback.getFatouratiTransactionNumber(),
                        callback.getChannel(),
                        callback.getOperator(),
                        callback.getAggregatorCode(),
                        callback.getPaymentSystemTransactionNumber()
                );
            } else {
                fretManagementNotifier.notifyPaymentConfirmed(
                        callback.getTokenRef(),
                        token.getMouvementId(),
                        callback.getTotalAmount(),
                        callback.getCurrency(),
                        callback.getFatouratiTransactionNumber()
                );
            }
        } else if (callback.getDecisionCode() == 1) {
            tokenRepository.recordTransition(
                    callback.getTokenRef(),
                    token.getStatus(),
                    FatouratiTokenStatus.REJECTED,
                    "PAYMENT_REFUSED",
                    "CMI_WEBHOOK",
                    callback.getChannel(),
                    callback.getOperator()
            );
            saveTransaction(callback, receiptNumber, "REJECTED");
            log.info("[FATOURATI_CONFIRM] Payment refused: tokenRef={}", callback.getTokenRef());
        } else {
            tokenRepository.recordTransition(
                    callback.getTokenRef(),
                    token.getStatus(),
                    token.getStatus(),
                    "UNKNOWN_DECISION_CODE",
                    "CMI_WEBHOOK",
                    callback.getChannel(),
                    callback.getOperator()
            );
            saveTransaction(callback, receiptNumber, "UNKNOWN");
            log.warn("[FATOURATI_CONFIRM] Unknown decisionCode={} for tokenRef={}",
                    callback.getDecisionCode(), callback.getTokenRef());
        }

        return receiptNumber;
    }

    private void saveTransaction(FatouratiPaymentCallback callback, String receiptNumber, String status) {
        try {
            List<FatouratiTransaction.SelectedItem> items = null;
            if (callback.getSelectedItems() != null) {
                items = callback.getSelectedItems().stream()
                        .map(si -> FatouratiTransaction.SelectedItem.builder()
                                .id(si.getId())
                                .amount(si.getAmount())
                                .build())
                        .toList();
            }

            FatouratiTransaction tx = FatouratiTransaction.builder()
                    .tokenRef(callback.getTokenRef())
                    .aggregatorCode(callback.getAggregatorCode())
                    .channel(callback.getChannel())
                    .operator(callback.getOperator())
                    .terminalId(null)
                    .fatouratiTransactionNumber(callback.getFatouratiTransactionNumber())
                    .paymentSystemTransactionNumber(callback.getPaymentSystemTransactionNumber())
                    .paymentMode(callback.getPaymentMode())
                    .amount(callback.getTotalAmount())
                    .currency(callback.getCurrency())
                    .transactionDate(callback.getTransactionDate())
                    .receiptNumber(receiptNumber)
                    .status(status)
                    .selectedItems(items)
                    .rawPayload(toJson(callback))
                    .build();
            transactionRepository.save(tx);
            log.info("[FATOURATI_CONFIRM] Saved transaction record for tokenRef={}, receipt={}", callback.getTokenRef(), receiptNumber);
        } catch (Exception e) {
            log.warn("[FATOURATI_CONFIRM] Failed to save transaction record for tokenRef={}: {}", callback.getTokenRef(), e.getMessage());
        }
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

            String expected = signatureUtil.computeSignature(
                    signatureData,
                    cmiProperties.getSignatureAlgorithm(),
                    cmiProperties.getStoreApiKey());
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
