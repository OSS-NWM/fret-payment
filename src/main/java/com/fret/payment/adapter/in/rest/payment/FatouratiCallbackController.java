package com.fret.payment.adapter.in.rest.payment;

import com.fret.payment.adapter.in.rest.payment.dto.FatouratiCallbackDto;
import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.cmi.CmiSignatureUtil;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenStatusHistoryRepositoryAdapter;
import com.fret.payment.application.service.payment.CancelFatouratiPaymentService;
import com.fret.payment.application.service.payment.ConfirmFatouratiPaymentService;
import com.fret.payment.domain.model.payment.FatouratiPaymentCallback;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment/fatourati")
@RequiredArgsConstructor
@Tag(name = "CMI Webhooks", description = "Public signature-verified webhooks called by CMI for payment confirmation, cancellation and status polling")
public class FatouratiCallbackController {

    private final ConfirmFatouratiPaymentService confirmService;
    private final CancelFatouratiPaymentService cancelService;
    private final CmiSignatureUtil signatureUtil;
    private final CmiProperties cmiProperties;
    private final ObjectMapper objectMapper;
    private final FatouratiTokenRepositoryAdapter tokenRepository;
    private final FatouratiTokenStatusHistoryRepositoryAdapter historyRepository;

    @PostMapping("/callback")
    @Operation(
            summary = "CMI payment confirmation webhook",
            description = "Called by CMI when a payment is confirmed. Verifies the payment decision and notifies fret-management. "
                    + "Signature verification is performed internally — x-signature header is forwarded by CMI."
    )
    @ApiResponse(responseCode = "200", description = "Payment confirmed or already processed",
            content = @Content(schema = @Schema(example = "{\"receiptNumber\": \"1000300000071\"}")))
    @ApiResponse(responseCode = "400", description = "Invalid body or signature verification failed")
    public ResponseEntity<?> callback(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-signature", required = false) String signature
    ) {
        FatouratiPaymentCallback callback;
        try {
            FatouratiCallbackDto dto = objectMapper.readValue(rawBody, FatouratiCallbackDto.class);
            log.info("[FATOURATI_CALLBACK] Received: tokenRef={}, decisionCode={}, numTrx={}",
                    dto.getTokenRef(), dto.getDecisionCode(), dto.getFatouratiTransactionNumber());

            callback = FatouratiPaymentCallback.builder()
                    .merchantCode(dto.getMerchantCode())
                    .store(dto.getStore())
                    .aggregatorCode(dto.getAggregatorCode())
                    .tokenRef(dto.getTokenRef())
                    .orderId(dto.getOrderId())
                    .totalAmount(dto.getTotalAmount())
                    .currency(dto.getCurrency())
                    .selectedItems(dto.getSelectedItems() != null
                            ? dto.getSelectedItems().stream().map(si ->
                                FatouratiPaymentCallback.SelectedItem.builder()
                                        .id(si.getId())
                                        .amount(si.getAmount())
                                        .build()).toList()
                            : null)
                    .transactionDate(parseDateTime(dto.getTransactionDate()))
                    .fatouratiTransactionNumber(dto.getFatouratiTransactionNumber())
                    .paymentSystemTransactionNumber(dto.getPaymentSystemTransactionNumber())
                    .paymentMode(dto.getPaymentMode())
                    .channel(dto.getChannel())
                    .operator(dto.getOperator())
                    .terminalId(null)
                    .extraData(dto.getExtraData())
                    .decisionCode(dto.getDecisionCode() != null ? dto.getDecisionCode() : 0)
                    .signature(signature)
                    .build();
        } catch (Exception e) {
            log.error("[FATOURATI_CALLBACK] Failed to parse callback body: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "INVALID_BODY",
                    "message", "Could not parse callback body",
                    "reference", UUID.randomUUID().toString(),
                    "timestamp", Instant.now().toString()
            ));
        }

        String result = confirmService.confirmPayment(callback);

        if (result != null && !result.isBlank() && !"3".equals(result)) {
            return ResponseEntity.ok(Map.of("receiptNumber", result));
        } else if ("2".equals(result) || "ALREADY_PROCESSED".equals(result)) {
            return ResponseEntity.ok(Map.of("receiptNumber", "ALREADY_PROCESSED"));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "SIGNATURE_INVALID",
                    "message", "Signature verification failed",
                    "reference", UUID.randomUUID().toString(),
                    "timestamp", Instant.now().toString()
            ));
        }
    }

    @GetMapping("/check-status")
    @Operation(
            summary = "CMI status polling endpoint",
            description = "Called by CMI to check the payment status of a token. Returns one of: PAID, CANCELLED, EXPIRED, NOT_FOUND, PENDING."
    )
    @ApiResponse(responseCode = "200", description = "Status returned",
            content = @Content(schema = @Schema(example = "{\"status\": \"PAID\"}")))
    public ResponseEntity<?> checkStatus(
            @RequestParam(value = "token_ref") String tokenRef,
            @RequestParam(value = "order_id", required = false) String orderId
    ) {
        log.info("[FATOURATI_CHECK_STATUS] Status check: tokenRef={}, orderId={}", tokenRef, orderId);

        String status = "PENDING";
        if (tokenRef != null && !tokenRef.isBlank()) {
            var tokenOpt = cancelService.getTokenForStatus(tokenRef);
            if (tokenOpt.isPresent()) {
                var token = tokenOpt.get();
                if (token.getStatus() == FatouratiTokenStatus.CONSUMED) {
                    status = "PAID";
                } else if (token.getStatus() == FatouratiTokenStatus.CANCELLED) {
                    status = "CANCELLED";
                } else if (token.getStatus() == FatouratiTokenStatus.EXPIRED) {
                    status = "EXPIRED";
                }
            } else {
                status = "NOT_FOUND";
            }
        }

        return ResponseEntity.ok(Map.of("status", status));
    }

    @PostMapping("/cancel")
    @Operation(
            summary = "CMI cancel webhook",
            description = "Called by CMI when a payment is cancelled. Verifies the cancel request signature before processing. "
                    + "x-signature header is required."
    )
    @ApiResponse(responseCode = "200", description = "Cancel processed successfully")
    @ApiResponse(responseCode = "400", description = "Missing signature, invalid body, or signature verification failed")
    public ResponseEntity<?> cancel(
            @RequestBody String rawBody,
            @RequestHeader(value = "x-signature", required = false) String signature
    ) {
        FatouratiCancelDto dto;
        try {
            dto = objectMapper.readValue(rawBody, FatouratiCancelDto.class);
        } catch (Exception e) {
            log.error("[FATOURATI_CANCEL] Failed to parse cancel body: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "INVALID_BODY",
                    "message", "Could not parse cancel body",
                    "reference", UUID.randomUUID().toString(),
                    "timestamp", Instant.now().toString()
            ));
        }

        log.info("[FATOURATI_CANCEL] Received: tokenRef={}, orderId={}",
                dto.getTokenRef(), dto.getOrderId());

        if (signature == null || signature.isBlank()) {
            log.warn("[FATOURATI_CANCEL] No x-signature in cancel request");
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "MISSING_SIGNATURE",
                    "message", "x-signature header is required",
                    "reference", UUID.randomUUID().toString(),
                    "timestamp", Instant.now().toString()
            ));
        }

        String totalAmount = dto.getTotalAmount() != null
                ? signatureUtil.formatAmount(dto.getTotalAmount()) : "";

        String signatureData = signatureUtil.buildCancelSignatureData(
                totalAmount,
                dto.getCurrency() != null ? dto.getCurrency() : "",
                cmiProperties.getMerchantCode() != null ? cmiProperties.getMerchantCode() : "",
                cmiProperties.getStore() != null ? cmiProperties.getStore() : "",
                dto.getTokenRef() != null ? dto.getTokenRef() : "",
                dto.getOrderId() != null ? dto.getOrderId() : "",
                dto.getFatouratiTransactionNumber() != null ? dto.getFatouratiTransactionNumber() : "",
                cmiProperties.getStoreApiKey() != null ? cmiProperties.getStoreApiKey() : ""
        );

        String expectedSig = signatureUtil.computeSignature(
                signatureData,
                cmiProperties.getSignatureAlgorithm(),
                cmiProperties.getStoreApiKey());
        if (!signatureUtil.constantTimeEquals(expectedSig, signature)) {
            log.warn("[FATOURATI_CANCEL] Signature mismatch for tokenRef={}", dto.getTokenRef());
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "SIGNATURE_INVALID",
                    "message", "Signature verification failed",
                    "reference", UUID.randomUUID().toString(),
                    "timestamp", Instant.now().toString()
            ));
        }

        if (dto.getOrderId() != null && !dto.getOrderId().isBlank()) {
            var tokenOpt = tokenRepository.findByMouvementId(dto.getOrderId());
            tokenOpt.ifPresent(token -> historyRepository.save(
                    com.fret.payment.domain.model.payment.FatouratiTokenStatusHistory.builder()
                            .tokenRef(token.getTokenRef())
                            .previousStatus(token.getStatus())
                            .newStatus(FatouratiTokenStatus.CANCELLED)
                            .reason("WEBHOOK_CANCEL")
                            .actor("CMI_WEBHOOK")
                            .build()
            ));
            cancelService.cancel(dto.getOrderId());
        }

        return ResponseEntity.ok(Map.of());
    }

    private LocalDateTime parseDateTime(String dateTime) {
        if (dateTime == null || dateTime.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return LocalDateTime.now();
            }
        }
    }

    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class FatouratiCancelDto {
        private String merchantCode;
        private String store;
        private String tokenRef;
        private String orderId;
        private BigDecimal totalAmount;
        private String currency;
        private String transactionDate;
        private String fatouratiTransactionNumber;
    }
}
