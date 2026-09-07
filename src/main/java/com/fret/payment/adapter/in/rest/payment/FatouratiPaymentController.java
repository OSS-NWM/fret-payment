package com.fret.payment.adapter.in.rest.payment;

import com.fret.payment.adapter.in.rest.payment.dto.FatouratiPaymentHistoryDto;
import com.fret.payment.adapter.in.rest.payment.dto.FatouratiStatusResponseDto;
import com.fret.payment.adapter.in.rest.payment.dto.FatouratiTokenResponseDto;
import com.fret.payment.adapter.out.cmi.CmiSignatureException;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenStatusHistoryRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.application.service.payment.CancelFatouratiPaymentService;
import com.fret.payment.application.service.payment.InitiateFatouratiPaymentService;
import com.fret.payment.application.service.payment.QueryFatouratiStatusService;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTokenStatusHistory;
import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment/fatourati")
@RequiredArgsConstructor
@Tag(name = "Fatourati Payments", description = "CMI Fatourati payment token lifecycle management")
@SecurityRequirement(name = "bearerAuth")
public class FatouratiPaymentController {

    private final InitiateFatouratiPaymentService initiateService;
    private final QueryFatouratiStatusService queryService;
    private final CancelFatouratiPaymentService cancelService;
    private final FatouratiTokenRepositoryAdapter tokenRepository;
    private final FatouratiTokenStatusHistoryRepositoryAdapter historyRepository;

    @PostMapping("/mouvement/{mouvementId}/paiement/fatourati")
    @PreAuthorize("hasAnyRole('OPERATEUR_COMMUNITY', 'AGENT_FACTURATION_NWM', 'RESPONSABLE_FACTURATION_NWM')")
    @Operation(
            summary = "Initiate a Fatourati payment",
            description = "Creates a CMI Fatourati payment token for the given mouvement. "
                    + "Fetches the invoice amount from fret-management, generates a CMI token, and returns QR code + payment channels."
    )
    @ApiResponse(responseCode = "200", description = "Token created successfully",
            content = @Content(schema = @Schema(implementation = FatouratiTokenResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "CMI rejected the request (signature or parameter error)")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    public ResponseEntity<?> initiatePayment(
            @Parameter(description = "Mouvement ID (e.g. AMI-202607000001)", example = "AMI-202607000001")
            @PathVariable String mouvementId) {
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
    @Operation(
            summary = "Get payment status for a mouvement",
            description = "Returns the current Fatourati token status and payment details."
    )
    @ApiResponse(responseCode = "200", description = "Status retrieved",
            content = @Content(schema = @Schema(implementation = FatouratiStatusResponseDto.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    public ResponseEntity<?> getPaymentStatus(
            @Parameter(description = "Mouvement ID", example = "AMI-202607000001")
            @PathVariable String mouvementId) {
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
    @Operation(
            summary = "Cancel a pending Fatourati payment",
            description = "Cancels the pending Fatourati token for the given mouvement. "
                    + "Admin-only action — writes a status history entry with the cancelling user's identity."
    )
    @ApiResponse(responseCode = "200", description = "Payment cancelled")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    public ResponseEntity<?> cancelPayment(
            @Parameter(description = "Mouvement ID", example = "AMI-202607000001")
            @PathVariable String mouvementId,
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getSubject() : "ADMIN";
        log.info("[FATOURATI_PAY] Cancel payment: mouvementId={}, actor={}", mouvementId, actor);

        var tokenOpt = tokenRepository.findActiveByMouvementId(mouvementId);
        tokenOpt.ifPresent(token -> historyRepository.save(
                FatouratiTokenStatusHistory.builder()
                        .tokenRef(token.getTokenRef())
                        .previousStatus(token.getStatus())
                        .newStatus(FatouratiTokenStatus.CANCELLED)
                        .reason("ADMIN_CANCEL")
                        .actor(actor)
                        .build()
        ));

        cancelService.cancel(mouvementId);

        return ResponseEntity.ok(Map.of(
                "mouvementId", mouvementId,
                "status", "CANCELLED"
        ));
    }

    @GetMapping("/mouvement/{mouvementId}/paiement/fatourati/history")
    @PreAuthorize("hasAnyRole('OPERATEUR_COMMUNITY', 'AGENT_FACTURATION_NWM', 'RESPONSABLE_FACTURATION_NWM')")
    @Operation(
            summary = "Get payment status history for a mouvement",
            description = "Returns the full status transition history for the Fatourati token associated with this mouvement."
    )
    @ApiResponse(responseCode = "200", description = "History retrieved")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    public ResponseEntity<?> getPaymentHistory(
            @Parameter(description = "Mouvement ID", example = "AMI-202607000001")
            @PathVariable String mouvementId) {
        log.info("[FATOURATI_PAY] History: mouvementId={}", mouvementId);

        var tokenOpt = tokenRepository.findByMouvementId(mouvementId);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.ok(java.util.List.of());
        }

        var token = tokenOpt.get();
        var history = historyRepository.findByTokenRef(token.getTokenRef()).stream()
                .map(h -> FatouratiPaymentHistoryDto.builder()
                        .id(h.getId())
                        .tokenRef(h.getTokenRef())
                        .previousStatus(h.getPreviousStatus() != null ? h.getPreviousStatus().name() : null)
                        .newStatus(h.getNewStatus().name())
                        .reason(h.getReason())
                        .actor(h.getActor())
                        .channel(h.getChannel())
                        .operator(h.getOperator())
                        .occurredAt(h.getOccurredAt())
                        .build())
                .toList();

        return ResponseEntity.ok(history);
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

    @ExceptionHandler(CmiSignatureException.class)
    public ResponseEntity<?> handleCmiSignatureError(CmiSignatureException ex) {
        log.error("[FATOURATI_PAY] CMI signature error: {}", ex.getMessage());
        HttpClientErrorException cause = (HttpClientErrorException) ex.getCause();
        return ResponseEntity.status(cause.getStatusCode())
                .body(Map.of(
                        "status", cause.getStatusCode().toString(),
                        "cmiError", cause.getResponseBodyAsString(),
                        "signatureData", ex.getSignatureData(),
                        "signatureValue", ex.getSignatureValue(),
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
