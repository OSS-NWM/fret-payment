package com.fret.payment.adapter.out.cmi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;
import com.fret.payment.domain.port.out.payment.CmiFatouratiClientPort;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class CmiFatouratiClientAdapter implements CmiFatouratiClientPort {

    private final CmiProperties props;
    private final CmiSignatureUtil signatureUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CmiAccessTokenCacheService tokenCache;

    public CmiFatouratiClientAdapter(CmiProperties props, CmiSignatureUtil signatureUtil,
                                     RestTemplate restTemplate, ObjectMapper objectMapper,
                                     @Lazy CmiAccessTokenCacheService tokenCache) {
        this.props = props;
        this.signatureUtil = signatureUtil;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.tokenCache = tokenCache;
    }

    @Override
    public String getAccessToken() {
        if (tokenCache != null) {
            return tokenCache.getAccessToken();
        }
        return requestAccessToken();
    }

    public String requestAccessToken() {
        String realmSegment = props.getAuthRealm() != null && !props.getAuthRealm().isBlank()
                ? "/realms/" + props.getAuthRealm()
                : "";
        String tokenUrl = props.getAuthUrl() + realmSegment + "/protocol/openid-connect/token";

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

        String currencyFinal = currency != null ? currency : "504";
        DateTimeFormatter cmiDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String orderDate = LocalDateTime.now(ZoneOffset.UTC).format(cmiDateFormat);
        String expiryDate = LocalDateTime.now(ZoneOffset.UTC).plusHours(24)
                .format(cmiDateFormat);

        GenerateTokenRequest.Item item = new GenerateTokenRequest.Item();
        item.setId(orderId);
        item.setAmount(amount);
        item.setDescription("Paiement AM Nador West Med");
        item.setDue(true);
        item.setSelected(true);

        GenerateTokenRequest.ClientInfo clientInfo = new GenerateTokenRequest.ClientInfo();
        clientInfo.setName(props.getClientName());
        clientInfo.setEmail(props.getClientEmail());
        clientInfo.setPhoneNumber(props.getClientPhone());
        clientInfo.setInfoToShow(List.of(
                new GenerateTokenRequest.InfoToShow("Facture", orderId)
        ));

        GenerateTokenRequest.OrderLinks orderLinks = new GenerateTokenRequest.OrderLinks();
        orderLinks.setCallbackURL(callbackUrl);
        orderLinks.setCheckStatusURL(checkStatusUrl);
        orderLinks.setCancelURL(cancelUrl);

        GenerateTokenRequest.ExtraData extraData = new GenerateTokenRequest.ExtraData();
        extraData.setKey("Type dossier");
        extraData.setValue("Autorisation mouvement portuaire");

        GenerateTokenRequest request = new GenerateTokenRequest();
        request.setOrderId(orderId);
        request.setOrderDate(orderDate);
        request.setTotalAmount(amount);
        request.setCurrency(currencyFinal);
        request.setPaymentMode("MULTI_CANAL");
        request.setPaymentType("TOTAL");
        request.setCashierId(props.getCashierId());
        request.setItems(List.of(item));
        request.setClientInfo(clientInfo);
        request.setCountryCode("MA");
        request.setOrderLinks(orderLinks);
        request.setAutoConfirmPayment(false);
        request.setIsCancellable(true);
        request.setGenerateQrCode(true);
        request.setExpiryDate(expiryDate);
        request.setLanguage("fr");

        String sigData = signatureUtil.buildTokenGenSignatureData(
                signatureUtil.formatAmount(amount),
                currencyFinal,
                props.getMerchantCode(),
                props.getStore(),
                orderId,
                props.getCashierId(),
                "MULTI_CANAL",
                "true",
                expiryDate,
                props.getStoreApiKey()
        );
        String signature;
        if ("SHA256".equalsIgnoreCase(props.getSignatureAlgorithm())) {
            signature = signatureUtil.computeSha256(sigData);
        } else {
            signature = signatureUtil.computeHmacSha256(sigData, props.getStoreApiKey());
        }
        log.info("[CMI] Signature data: '{}'", sigData);
        log.info("[CMI] x-signature algorithm: {}, value: {}", props.getSignatureAlgorithm(), signature);
        request.setExtraData(List.of(extraData));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(getAccessToken());
        headers.set("x-signature", signature);

        HttpEntity<GenerateTokenRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("[CMI] Generating token for orderId={}, amount={}", orderId, amount);
            ResponseEntity<GenerateTokenResponse> response = restTemplate.exchange(
                    URI.create(url), HttpMethod.POST, entity, GenerateTokenResponse.class);

            GenerateTokenResponse body = response.getBody();
            if (body == null) {
                throw new RuntimeException("Empty response from CMI generateToken");
            }

            log.info("[CMI] Token generated: tokenRef={}, qrCodePresent={}",
                    body.getTokenRef(), body.getQrCode() != null);

            List<String> channels = body.getRefPaymentSystems() != null
                    ? body.getRefPaymentSystems().stream()
                        .map(GenerateTokenResponse.RefPaymentSystem::getDescription)
                        .filter(Objects::nonNull)
                        .toList()
                    : List.of();

            LocalDateTime expiresAt = parseDateTime(expiryDate);

            return FatouratiToken.builder()
                    .tokenRef(body.getTokenRef())
                    .orderId(body.getOrderId() != null ? body.getOrderId() : orderId)
                    .totalAmount(amount)
                    .currency(currencyFinal)
                    .qrCode(body.getQrCode())
                    .channels(channels)
                    .expiresAt(expiresAt)
                    .status(parseTokenStatus(body.getStatus()))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("[CMI] generateToken failed: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new CmiSignatureException(
                    "CMI generateToken failed: " + e.getStatusCode(),
                    sigData,
                    signature,
                    e
            );
        }
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
        private String orderDate;
        private BigDecimal totalAmount;
        private String currency;
        private String paymentMode = "MULTI_CANAL";
        private String paymentType = "TOTAL";
        private String cashierId;
        private List<Item> items;
        private ClientInfo clientInfo;
        private String countryCode;
        private OrderLinks orderLinks;
        private Boolean autoConfirmPayment = false;
        private Boolean isCancellable = true;
        private Boolean generateQrCode = true;
        private String expiryDate;
        private String language;
        private List<ExtraData> extraData;

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Item {
            private String id;
            private BigDecimal amount;
            private String description;
            private Boolean due = true;
            private Boolean selected = true;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ClientInfo {
            private String name;
            private String email;
            private String phoneNumber;
            private List<InfoToShow> infoToShow;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class InfoToShow {
            private String key;
            private String value;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class OrderLinks {
            @JsonProperty("callbackURL") private String callbackURL;
            @JsonProperty("checkStatusURL") private String checkStatusURL;
            @JsonProperty("cancelURL") private String cancelURL;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ExtraData {
            private String key;
            private String value;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    private static class GenerateTokenResponse {
        @JsonProperty("status") private String status;
        @JsonProperty("orderId") private String orderId;
        @JsonProperty("tokenRef") private String tokenRef;
        @JsonProperty("qrCode") private String qrCode;
        @JsonProperty("extraData") private List<ExtraData> extraData;
        @JsonProperty("refPaymentSystems") private List<RefPaymentSystem> refPaymentSystems;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class ExtraData {
            @JsonProperty("key") private String key;
            @JsonProperty("value") private String value;
        }

        @Getter
        @Setter
        @NoArgsConstructor
        public static class RefPaymentSystem {
            @JsonProperty("description") private String description;
            @JsonProperty("urlSite") private String urlSite;
            @JsonProperty("urlLogo") private String urlLogo;
        }
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
