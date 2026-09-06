package com.fret.payment.adapter.in.rest.payment;

import com.fret.payment.adapter.in.rest.payment.dto.FatouratiStatusResponseDto;
import com.fret.payment.adapter.in.rest.payment.dto.FatouratiTokenResponseDto;
import com.fret.payment.application.service.payment.CancelFatouratiPaymentService;
import com.fret.payment.application.service.payment.InitiateFatouratiPaymentService;
import com.fret.payment.application.service.payment.QueryFatouratiStatusService;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment/fatourati")
@RequiredArgsConstructor
public class FatouratiPaymentController {

    private final InitiateFatouratiPaymentService initiateService;
    private final QueryFatouratiStatusService queryService;
    private final CancelFatouratiPaymentService cancelService;

    @PostMapping("/mouvement/{mouvementId}/paiement/fatourati")
    @PreAuthorize("hasAnyRole('OPERATEUR_COMMUNITY', 'AGENT_FACTURATION_NWM', 'RESPONSABLE_FACTURATION_NWM')")
    public ResponseEntity<?> initiatePayment(@PathVariable String mouvementId) {
        log.info("[FATOURATI_PAY] Initiate payment: mouvementId={}", mouvementId);

        FatouratiToken token = initiateService.initiate(mouvementId);

        FatouratiTokenResponseDto dto = FatouratiTokenResponseDto.builder()
                .tokenRef(token.getTokenRef())
                .mouvementId(token.getMouvementId())
                .orderId(token.getOrderId())
                .totalAmount(token.getTotalAmount())
                .currency(token.getCurrency())
                .qrCode(token.getQrCode())
                .channels(token.getChannels())
                .expiresAt(token.getExpiresAt())
                .status(token.getStatus() != null ? token.getStatus().name() : "CREATED")
                .build();

        return ResponseEntity.ok(dto);
    }

    @GetMapping("/mouvement/{mouvementId}/paiement/fatourati/status")
    @PreAuthorize("hasAnyRole('OPERATEUR_COMMUNITY', 'AGENT_FACTURATION_NWM', 'RESPONSABLE_FACTURATION_NWM')")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String mouvementId) {
        log.debug("[FATOURATI_PAY] Status check: mouvementId={}", mouvementId);

        FatouratiToken token = queryService.getToken(mouvementId);
        FatouratiTransactionStatus status = queryService.getStatus(mouvementId);

        FatouratiStatusResponseDto dto = FatouratiStatusResponseDto.builder()
                .mouvementId(mouvementId)
                .status(status.name())
                .tokenRef(token != null ? token.getTokenRef() : null)
                .totalAmount(token != null ? token.getTotalAmount() : null)
                .channels(token != null ? token.getChannels() : null)
                .expiresAt(token != null && token.getExpiresAt() != null
                        ? token.getExpiresAt().toString() : null)
                .build();

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/mouvement/{mouvementId}/paiement/fatourati")
    @PreAuthorize("hasAnyRole('OPERATEUR_COMMUNITY', 'AGENT_FACTURATION_NWM', 'RESPONSABLE_FACTURATION_NWM')")
    public ResponseEntity<?> cancelPayment(@PathVariable String mouvementId) {
        log.info("[FATOURATI_PAY] Cancel payment: mouvementId={}", mouvementId);

        cancelService.cancel(mouvementId);

        return ResponseEntity.ok(Map.of(
                "mouvementId", mouvementId,
                "status", "CANCELLED"
        ));
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<?> handleHttpClientError(HttpClientErrorException ex) {
        log.error("[FATOURATI_PAY] CMI HTTP error: {} {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of(
                        "status", ex.getStatusCode().toString(),
                        "cmiError", ex.getResponseBodyAsString(),
                        "path", "/api/payment/fatourati"
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeError(RuntimeException ex) {
        log.error("[FATOURATI_PAY] Runtime error: {}", ex.getMessage());
        return ResponseEntity.status(500)
                .body(Map.of(
                        "error", ex.getMessage(),
                        "type", ex.getClass().getSimpleName(),
                        "path", "/api/payment/fatourati"
                ));
    }
}
