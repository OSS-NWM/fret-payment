package com.fret.payment.adapter.out.cmi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;
import com.fret.payment.domain.port.out.payment.CmiFatouratiClientPort;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class CmiFatouratiClientAdapter implements CmiFatouratiClientPort {

    private final CmiProperties props;
    private final CmiSignatureUtil signatureUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CmiFatouratiClientAdapter(CmiProperties props, CmiSignatureUtil signatureUtil,
                                     RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.props = props;
        this.signatureUtil = signatureUtil;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getAccessToken() {
        return requestAccessToken();
    }

    public String requestAccessToken() {
        String tokenUrl = props.getAuthUrl() + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = String.join("&",
                "grant_type=client_credentials",
                "client_id=" + props.getClientId(),
                "client_secret=" + props.getClientSecret()
        );

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<OAuthTokenResponse> response = restTemplate.exchange(
                    URI.create(tokenUrl), HttpMethod.POST, entity, OAuthTokenResponse.class);

            if (response.getBody() != null) {
                log.info("[CMI] Access token obtained, expires in {}s", response.getBody().getExpiresIn());
                return response.getBody().getAccessToken();
            }
            throw new RuntimeException("Empty OAuth token response from CMI");
        } catch (HttpClientErrorException e) {
            log.error("[CMI] OAuth token request failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("CMI OAuth token request failed: " + e.getStatusCode(), e);
        }
    }

    @Override
    public FatouratiToken generateToken(String orderId, BigDecimal amount, String currency,
                                         String callbackUrl, String cancelUrl, String checkStatusUrl) {
        String url = props.getBaseUrl() + "/api/" + props.getApiVersion()
                + "/merchants/" + props.getMerchantCode()
                + "/stores/" + props.getStore() + "/token";

        String amountStr = signatureUtil.formatAmount(amount);
        String expiryDate = LocalDateTime.now().plusHours(24)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String signatureData = signatureUtil.buildTokenGenSignatureData(
                amountStr,
                currency != null ? currency : "504",
                props.getMerchantCode(),
                props.getStore(),
                orderId,
                props.getCashierId(),
                "MULTI_CANAL",
                "0",
                expiryDate,
                props.getStoreApiKey()
        );
        String signature = signatureUtil.computeSignature(signatureData, props.getStoreApiKey());

        GenerateTokenRequest.TokenItem item = new GenerateTokenRequest.TokenItem();
        item.setId(orderId);
        item.setAmount(amount);
        item.setDescription("Paiement AM Nador West Med");
        item.setDue(true);
        item.setSelected(true);

        GenerateTokenRequest request = new GenerateTokenRequest();
        request.setOrderId(orderId);
        request.setTotalAmount(amount);
        request.setCurrency(currency != null ? currency : "504");
        request.setCashierId(props.getCashierId());
        request.setPaymentMode("MULTI_CANAL");
        request.setItems(List.of(item));
        request.setIsCancellable(true);
        request.setGenerateQrCode(true);
        request.setExpiryDate(expiryDate);
        request.setCallbackUrl(callbackUrl);
        request.setCancelUrl(cancelUrl);
        request.setCheckStatusUrl(checkStatusUrl);
        request.setSignature(signature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());

        HttpEntity<GenerateTokenRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[CMI] Generating token for orderId={}, amount={}", orderId, amount);
            ResponseEntity<GenerateTokenResponse> response = restTemplate.exchange(
                    URI.create(url), HttpMethod.POST, entity, GenerateTokenResponse.class);

            GenerateTokenResponse body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from CMI generateToken");
            }

            log.info("[CMI] Token generated: tokenRef={}", body.getTokenRef());

            FatouratiToken partial = FatouratiToken.builder()
                    .tokenRef(body.getTokenRef())
                    .orderId(body.getOrderId() != null ? body.getOrderId() : orderId)
                    .totalAmount(amount)
                    .currency(currency != null ? currency : "504")
                    .status(parseTokenStatus(body.getStatus()))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return enrichTokenFromGetToken(partial);

        } catch (HttpClientErrorException e) {
            log.error("[CMI] generateToken failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("CMI generateToken failed: " + e.getStatusCode(), e);
        }
    }

    private FatouratiToken enrichTokenFromGetToken(FatouratiToken partial) {
        try {
            FatouratiToken enriched = getTokenByRef(partial.getTokenRef());
            if (enriched != null) {
                enriched.setMouvementId(partial.getMouvementId());
                enriched.setTotalAmount(partial.getTotalAmount());
                enriched.setCurrency(partial.getCurrency());
                enriched.setStatus(partial.getStatus());
                enriched.setCreatedAt(partial.getCreatedAt());
                enriched.setUpdatedAt(LocalDateTime.now());
                log.info("[CMI] Token enriched via getTokenByRef: tokenRef={}, qrCode present={}, channels={}",
                        enriched.getTokenRef(), enriched.getQrCode() != null,
                        enriched.getChannels() != null ? enriched.getChannels().size() : 0);
                return enriched;
            }
        } catch (Exception e) {
            log.warn("[CMI] Could not enrich token via getTokenByRef: {}", e.getMessage());
        }
        partial.setQrCode(null);
        partial.setChannels(List.of());
        partial.setExpiresAt(LocalDateTime.now().plusHours(24));
        return partial;
    }

    @Override
    public FatouratiToken getTokenByRef(String tokenRef) {
        String url = props.getBaseUrl() + "/api/" + props.getApiVersion()
                + "/merchants/" + props.getMerchantCode()
                + "/stores/" + props.getStore() + "/tokens/" + tokenRef;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());

        try {
            ResponseEntity<TokenStatusResponse> response = restTemplate.exchange(
                    URI.create(url), HttpMethod.GET, new HttpEntity<>(headers), TokenStatusResponse.class);

            TokenStatusResponse body = response.getBody();
            if (body == null) {
                return null;
            }

            return FatouratiToken.builder()
                    .tokenRef(body.getTokenRef())
                    .orderId(body.getOrderId())
                    .totalAmount(body.getAmount())
                    .currency(body.getCurrency())
                    .status(parseTokenStatus(body.getStatus()))
                    .expiresAt(parseDateTime(body.getExpiresAt()))
                    .channels(body.getChannels())
                    .qrCode(body.getQrcode())
                    .build();

        } catch (HttpClientErrorException e) {
            log.warn("[CMI] getTokenByRef failed: {} {}", e.getStatusCode(), e.getMessage());
            return null;
        }
    }

    @Override
    public FatouratiToken getTokenByOrderId(String orderId) {
        String url = props.getBaseUrl() + "/api/" + props.getApiVersion()
                + "/merchants/" + props.getMerchantCode()
                + "/stores/" + props.getStore() + "/orders/" + orderId + "/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());

        try {
            ResponseEntity<TokenStatusResponse> response = restTemplate.exchange(
                    URI.create(url), HttpMethod.GET, new HttpEntity<>(headers), TokenStatusResponse.class);

            TokenStatusResponse body = response.getBody();
            if (body == null) {
                return null;
            }

            return FatouratiToken.builder()
                    .tokenRef(body.getTokenRef())
                    .orderId(body.getOrderId())
                    .totalAmount(body.getAmount())
                    .currency(body.getCurrency())
                    .status(parseTokenStatus(body.getStatus()))
                    .expiresAt(parseDateTime(body.getExpiresAt()))
                    .channels(body.getChannels())
                    .qrCode(body.getQrcode())
                    .build();

        } catch (HttpClientErrorException e) {
            log.warn("[CMI] getTokenByOrderId failed: {} {}", e.getStatusCode(), e.getMessage());
            return null;
        }
    }

    @Override
    public FatouratiTransactionStatus getTransactionStatus(String fatouratiTransactionNumber) {
        String url = props.getBaseUrl() + "/api/" + props.getApiVersion()
                + "/merchants/" + props.getMerchantCode()
                + "/stores/" + props.getStore() + "/transactions/" + fatouratiTransactionNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());

        try {
            ResponseEntity<TransactionStatusResponse> response = restTemplate.exchange(
                    URI.create(url), HttpMethod.GET, new HttpEntity<>(headers), TransactionStatusResponse.class);

            TransactionStatusResponse body = response.getBody();
            if (body == null) {
                return FatouratiTransactionStatus.NOT_FOUND;
            }

            return parseTransactionStatus(body.getStatus());

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                return FatouratiTransactionStatus.NOT_FOUND;
            }
            log.error("[CMI] getTransactionStatus failed: {} {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException("CMI getTransactionStatus failed", e);
        }
    }

    @Override
    public void updateTransactionStatus(String fatouratiTransactionNumber, FatouratiTransactionStatus status) {
        log.warn("[CMI] updateTransactionStatus not implemented — CMI does not support merchant-initiated status updates");
    }

    @Override
    public void cancelToken(String tokenRef) {
        log.warn("[CMI] cancelToken called but outbound cancel is not supported in v1.2.4 — cancel is inbound only from CMI");
    }

    @Override
    public List<String> getChannels(String tokenRef) {
        FatouratiToken token = getTokenByRef(tokenRef);
        if (token != null && token.getChannels() != null) {
            return token.getChannels();
        }
        return List.of();
    }

    private LocalDateTime parseDateTime(String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(isoDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                return LocalDateTime.now().plusHours(24);
            }
        }
    }

    private FatouratiTokenStatus parseTokenStatus(String status) {
        if (status == null) {
            return FatouratiTokenStatus.CREATED;
        }
        return switch (status.toUpperCase()) {
            case "SUCCESS", "CREATED" -> FatouratiTokenStatus.CREATED;
            case "CONSUMED" -> FatouratiTokenStatus.CONSUMED;
            case "EXPIRED" -> FatouratiTokenStatus.EXPIRED;
            case "CANCELLED" -> FatouratiTokenStatus.CANCELLED;
            default -> FatouratiTokenStatus.CREATED;
        };
    }

    private FatouratiTransactionStatus parseTransactionStatus(String status) {
        if (status == null) {
            return FatouratiTransactionStatus.PENDING;
        }
        return switch (status.toUpperCase()) {
            case "PAID", "COMPLETED" -> FatouratiTransactionStatus.PAID;
            case "CANCELLED", "CANCELED" -> FatouratiTransactionStatus.CANCELLED;
            case "REJECTED", "FAILED" -> FatouratiTransactionStatus.REJECTED;
            default -> FatouratiTransactionStatus.PENDING;
        };
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class OAuthTokenResponse {
        @JsonProperty("access_token") private String accessToken;
        @JsonProperty("token_type") private String tokenType;
        @JsonProperty("expires_in") private Integer expiresIn;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GenerateTokenRequest {
        private String orderId;
        private BigDecimal totalAmount;
        private String currency;
        private String cashierId;
        private String paymentMode = "MULTI_CANAL";
        private List<TokenItem> items;
        private Boolean isCancellable = true;
        private Boolean generateQrCode = true;
        private String expiryDate;
        @JsonProperty("callbackURL") private String callbackUrl;
        @JsonProperty("cancelURL") private String cancelUrl;
        @JsonProperty("checkStatusURL") private String checkStatusUrl;
        private String signature;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        private static class TokenItem {
            private String id;
            private BigDecimal amount;
            private String description;
            private Boolean due = true;
            private Boolean selected = true;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class GenerateTokenResponse {
        @JsonProperty("status") private String status;
        @JsonProperty("orderId") private String orderId;
        @JsonProperty("tokenRef") private String tokenRef;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class TokenStatusResponse {
        @JsonProperty("tokenRef") private String tokenRef;
        @JsonProperty("orderId") private String orderId;
        @JsonProperty("amount") private BigDecimal amount;
        @JsonProperty("currency") private String currency;
        @JsonProperty("status") private String status;
        @JsonProperty("expiresAt") private String expiresAt;
        @JsonProperty("createdAt") private String createdAt;
        @JsonProperty("updatedAt") private String updatedAt;
        @JsonProperty("paidAmount") private BigDecimal paidAmount;
        @JsonProperty("refundedAmount") private BigDecimal refundedAmount;
        @JsonProperty("qrcode") private String qrcode;
        @JsonProperty("transactions") private List<Object> transactions;
        @JsonProperty("channels") private List<String> channels;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class TransactionStatusResponse {
        @JsonProperty("orderId") private String orderId;
        @JsonProperty("merchantCode") private String merchantCode;
        @JsonProperty("store") private String store;
        @JsonProperty("currency") private String currency;
        @JsonProperty("transactionDate") private String transactionDate;
        @JsonProperty("fatouratiTransactionNumber") private String fatouratiTransactionNumber;
        @JsonProperty("status") private String status;
    }
}
