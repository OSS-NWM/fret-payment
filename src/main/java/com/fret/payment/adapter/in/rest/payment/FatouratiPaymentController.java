package com.fret.payment.adapter.in.rest.payment;

import com.fret.payment.adapter.in.rest.payment.dto.FatouratiPaymentHistoryDto;
import com.fret.payment.adapter.in.rest.payment.dto.FatouratiPaymentTransactionDto;
import com.fret.payment.adapter.in.rest.payment.dto.FatouratiStatusResponseDto;
import com.fret.payment.adapter.in.rest.payment.dto.FatouratiTokenResponseDto;
import com.fret.payment.adapter.in.rest.payment.dto.InitiateGroupPaymentRequest;
import com.fret.payment.adapter.out.cmi.CmiSignatureException;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenStatusHistoryRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTransactionRepositoryAdapter;
import com.fret.payment.application.service.payment.CancelFatouratiPaymentService;
import com.fret.payment.application.service.payment.InitiateFatouratiPaymentService;
import com.fret.payment.application.service.payment.QueryFatouratiStatusService;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTokenStatusHistory;
import com.fret.payment.domain.model.payment.FatouratiTransaction;
import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

    private static final String FATOURATI_ROLES =
            "hasAnyRole('OPERATEUR_COMMUNITY', 'AGENT_FACTURATION_NWM', 'RESPONSABLE_FACTURATION_NWM')";

    private final InitiateFatouratiPaymentService initiateService;
    private final QueryFatouratiStatusService queryService;
    private final CancelFatouratiPaymentService cancelService;
    private final FatouratiTokenRepositoryAdapter tokenRepository;
    private final FatouratiTokenStatusHistoryRepositoryAdapter historyRepository;
    private final FatouratiTransactionRepositoryAdapter transactionRepository;

    @PostMapping("/invoices/{invoiceId}/paiement/fatourati")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "Initiate a Fatourati payment for an invoice",
            description = "Creates a CMI Fatourati payment token for the given invoice. "
                    + "Fetches invoice lines from fret-management, builds CMI items from each line, and returns QR code + payment channels."
    )
    @ApiResponse(responseCode = "200", description = "Token created successfully",
            content = @Content(schema = @Schema(implementation = FatouratiTokenResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invoice not found or has no lines")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error",
            content = @Content(schema = @Schema(example = "{\"error\":\"CMI access token refresh failed\",\"type\":\"RuntimeException\",\"path\":\"/api/payment/fatourati\"}")))
    public ResponseEntity<?> initiatePayment(
            @Parameter(description = "Invoice ID (idHeader)", example = "1001")
            @PathVariable Long invoiceId) {
        log.info("[FATOURATI_PAY] Initiate payment: invoiceId={}", invoiceId);

        FatouratiToken token = initiateService.initiate(invoiceId);
        return ResponseEntity.ok(toTokenResponse(token));
    }

    @PostMapping("/paiements/fatourati/group")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "Initiate a Fatourati payment for multiple invoices",
            description = "Creates a single CMI Fatourati token covering all specified invoices. "
                    + "All invoice lines are aggregated into one payment request."
    )
    @ApiResponse(responseCode = "200", description = "Group token created successfully",
            content = @Content(schema = @Schema(implementation = FatouratiTokenResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "No invoice IDs provided or invoice not found")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> initiateGroupPayment(@RequestBody InitiateGroupPaymentRequest request) {
        log.info("[FATOURATI_PAY] Initiate group payment: invoiceIds={}", request.getInvoiceIds());

        FatouratiToken token = initiateService.initiateGroup(request.getInvoiceIds());
        return ResponseEntity.ok(toTokenResponse(token));
    }

    @GetMapping("/invoices/{invoiceId}/paiement/fatourati/status")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "Get payment status for an invoice",
            description = "Returns the current Fatourati token status and payment details."
    )
    @ApiResponse(responseCode = "200", description = "Status retrieved",
            content = @Content(schema = @Schema(implementation = FatouratiStatusResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Invalid invoice ID")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> getPaymentStatus(
            @Parameter(description = "Invoice ID", example = "1001")
            @PathVariable Long invoiceId) {
        log.debug("[FATOURATI_PAY] Status check: invoiceId={}", invoiceId);

        FatouratiToken token = queryService.getTokenByInvoiceId(invoiceId);
        FatouratiTransactionStatus status = queryService.getStatusByInvoiceId(invoiceId);

        FatouratiStatusResponseDto dto = FatouratiStatusResponseDto.builder()
                .invoiceId(invoiceId)
                .status(status.name())
                .tokenRef(token != null ? token.getTokenRef() : null)
                .totalAmount(token != null ? token.getTotalAmount() : null)
                .channels(token != null ? token.getChannels() : null)
                .expiresAt(token != null && token.getExpiresAt() != null
                        ? token.getExpiresAt().toString() : null)
                .qrCode(token != null ? token.getQrCode() : null)
                .build();

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/invoices/{invoiceId}/paiement/fatourati")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "Cancel a pending Fatourati payment for an invoice",
            description = "Cancels the pending Fatourati token for the given invoice."
    )
    @ApiResponse(responseCode = "200", description = "Payment cancelled")
    @ApiResponse(responseCode = "400", description = "Invoice not found or already cancelled")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> cancelPayment(
            @Parameter(description = "Invoice ID", example = "1001")
            @PathVariable Long invoiceId,
            @Parameter(description = "Authenticated JWT principal")
            @AuthenticationPrincipal Jwt jwt) {
        String actor = jwt != null ? jwt.getSubject() : "ADMIN";
        log.info("[FATOURATI_PAY] Cancel payment: invoiceId={}, actor={}", invoiceId, actor);

        var tokenOpt = tokenRepository.findActiveByInvoiceId(invoiceId);
        tokenOpt.ifPresent(token -> historyRepository.save(
                FatouratiTokenStatusHistory.builder()
                        .tokenRef(token.getTokenRef())
                        .previousStatus(token.getStatus())
                        .newStatus(FatouratiTokenStatus.CANCELLED)
                        .reason("ADMIN_CANCEL")
                        .actor(actor)
                        .build()
        ));

        cancelService.cancelByInvoiceId(invoiceId, actor);

        return ResponseEntity.ok(Map.of(
                "invoiceId", invoiceId,
                "status", "CANCELLED"
        ));
    }

    @GetMapping("/invoices/{invoiceId}/paiement/fatourati/history")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "Get payment status history for an invoice",
            description = "Returns the full status transition history for the Fatourati token associated with this invoice."
    )
    @ApiResponse(responseCode = "200", description = "History retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid invoice ID")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> getPaymentHistory(
            @Parameter(description = "Invoice ID", example = "1001")
            @PathVariable Long invoiceId) {
        log.info("[FATOURATI_PAY] History: invoiceId={}", invoiceId);

        var tokenOpt = tokenRepository.findActiveByInvoiceId(invoiceId);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.ok(java.util.List.of());
        }

        var token = tokenOpt.get();
        var history = historyRepository.findByTokenRef(token.getTokenRef()).stream()
                .map(this::toHistoryDto)
                .toList();

        return ResponseEntity.ok(history);
    }

    @GetMapping("/invoices/{invoiceId}/paiement/fatourati/transactions")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "Get all CMI transaction records for an invoice",
            description = "Returns the full list of Fatourati transactions (CMI callbacks) for this invoice's token."
    )
    @ApiResponse(responseCode = "200", description = "Transactions retrieved")
    @ApiResponse(responseCode = "400", description = "Invalid invoice ID")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> getPaymentTransactions(
            @Parameter(description = "Invoice ID", example = "1001")
            @PathVariable Long invoiceId) {
        log.info("[FATOURATI_PAY] Transactions: invoiceId={}", invoiceId);

        var tokenOpt = tokenRepository.findActiveByInvoiceId(invoiceId);
        if (tokenOpt.isEmpty()) {
            return ResponseEntity.ok(java.util.List.of());
        }

        var token = tokenOpt.get();
        var transactions = transactionRepository.findByTokenRef(token.getTokenRef()).stream()
                .map(this::toTransactionDto)
                .toList();

        return ResponseEntity.ok(transactions);
    }

    @Deprecated
    @PostMapping("/mouvement/{mouvementId}/paiement/fatourati")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "[DEPRECATED] Initiate a Fatourati payment for a mouvement",
            description = "Deprecated — use POST /invoices/{invoiceId}/paiement/fatourati instead. "
                    + "This endpoint resolves the latest invoice for the mouvement and redirects."
    )
    @ApiResponse(responseCode = "200", description = "Token created (via latest invoice)")
    @ApiResponse(responseCode = "400", description = "No invoice found for this mouvement")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> initiatePaymentLegacy(
            @Parameter(description = "Mouvement ID (deprecated)", example = "AMI-202607000001")
            @PathVariable String mouvementId) {
        log.warn("[FATOURATI_PAY] [DEPRECATED] /mouvement/{}/paiement/fatourati called — use /invoices/{{invoiceId}}/paiement/fatourati", mouvementId);
        throw new UnsupportedOperationException("Deprecated endpoint — use /invoices/{invoiceId}/paiement/fatourati");
    }

    @Deprecated
    @GetMapping("/mouvement/{mouvementId}/paiement/fatourati/status")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "[DEPRECATED] Get payment status for a mouvement",
            description = "Deprecated — use GET /invoices/{invoiceId}/paiement/fatourati/status instead."
    )
    @ApiResponse(responseCode = "200", description = "Status retrieved")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> getPaymentStatusLegacy(
            @Parameter(description = "Mouvement ID (deprecated)", example = "AMI-202607000001")
            @PathVariable String mouvementId) {
        log.warn("[FATOURATI_PAY] [DEPRECATED] /mouvement/{}/paiement/fatourati/status called — use /invoices/{{invoiceId}}/paiement/fatourati/status", mouvementId);
        throw new UnsupportedOperationException("Deprecated endpoint — use /invoices/{invoiceId}/paiement/fatourati/status");
    }

    @Deprecated
    @DeleteMapping("/mouvement/{mouvementId}/paiement/fatourati")
    @PreAuthorize(FATOURATI_ROLES)
    @Operation(
            summary = "[DEPRECATED] Cancel a pending Fatourati payment for a mouvement",
            description = "Deprecated — use DELETE /invoices/{invoiceId}/paiement/fatourati instead."
    )
    @ApiResponse(responseCode = "200", description = "Payment cancelled")
    @ApiResponse(responseCode = "400", description = "Mouvement not found or already cancelled")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    @ApiResponse(responseCode = "403", description = "Insufficient role permissions")
    @ApiResponse(responseCode = "500", description = "Internal server error")
    public ResponseEntity<?> cancelPaymentLegacy(
            @Parameter(description = "Mouvement ID (deprecated)", example = "AMI-202607000001")
            @PathVariable String mouvementId,
            @AuthenticationPrincipal Jwt jwt) {
        log.warn("[FATOURATI_PAY] [DEPRECATED] /mouvement/{}/paiement/fatourati DELETE called — use /invoices/{{invoiceId}}/paiement/fatourati", mouvementId);
        throw new UnsupportedOperationException("Deprecated endpoint — use /invoices/{invoiceId}/paiement/fatourati");
    }

    private FatouratiTokenResponseDto toTokenResponse(FatouratiToken token) {
        return FatouratiTokenResponseDto.builder()
                .tokenRef(token.getTokenRef())
                .invoiceId(token.getInvoiceId())
                .mouvementId(token.getMouvementId())
                .orderId(token.getOrderId())
                .totalAmount(token.getTotalAmount())
                .currency(token.getCurrency())
                .qrCode(token.getQrCode())
                .channels(token.getChannels())
                .expiresAt(token.getExpiresAt())
                .status(token.getStatus() != null ? token.getStatus().name() : "CREATED")
                .build();
    }

    private FatouratiPaymentHistoryDto toHistoryDto(FatouratiTokenStatusHistory h) {
        return FatouratiPaymentHistoryDto.builder()
                .id(h.getId())
                .tokenRef(h.getTokenRef())
                .previousStatus(h.getPreviousStatus() != null ? h.getPreviousStatus().name() : null)
                .newStatus(h.getNewStatus().name())
                .reason(h.getReason())
                .actor(h.getActor())
                .channel(h.getChannel())
                .operator(h.getOperator())
                .occurredAt(h.getOccurredAt())
                .build();
    }

    private FatouratiPaymentTransactionDto toTransactionDto(FatouratiTransaction t) {
        return FatouratiPaymentTransactionDto.builder()
                .id(t.getId())
                .tokenRef(t.getTokenRef())
                .aggregatorCode(t.getAggregatorCode())
                .channel(t.getChannel())
                .operator(t.getOperator())
                .terminalId(t.getTerminalId())
                .fatouratiTransactionNumber(t.getFatouratiTransactionNumber())
                .paymentSystemTransactionNumber(t.getPaymentSystemTransactionNumber())
                .paymentMode(t.getPaymentMode())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .transactionDate(t.getTransactionDate())
                .receiptNumber(t.getReceiptNumber())
                .status(t.getStatus())
                .selectedItems(t.getSelectedItems() != null
                        ? t.getSelectedItems().stream().map(si ->
                            FatouratiPaymentTransactionDto.SelectedItem.builder()
                                    .id(si.getId())
                                    .amount(si.getAmount())
                                    .build()).toList()
                        : null)
                .createdAt(t.getCreatedAt())
                .build();
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
