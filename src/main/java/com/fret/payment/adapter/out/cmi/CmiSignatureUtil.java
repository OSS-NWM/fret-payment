package com.fret.payment.adapter.out.cmi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;
import java.util.Arrays;

@Slf4j
@Component
public class CmiSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public String computeSignature(String data, String secretKey) {
        return computeHmacSha256(data, secretKey);
    }

    /**
     * Compute signature using the algorithm specified. Supports:
     * - "SHA256" (case-insensitive) → plain SHA-256 (no key)
     * - "HMAC-SHA256" / anything else → HMAC-SHA-256 with key
     *
     * Used for callback verification which must respect the configured algorithm
     * the same way token generation does.
     */
    public String computeSignature(String data, String algorithm, String secretKey) {
        if ("SHA256".equalsIgnoreCase(algorithm)) {
            return computeSha256(data);
        }
        return computeHmacSha256(data, secretKey);
    }

    public String computeHmacSha256(String data, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[CMI] Failed to compute HMAC-SHA256 signature: {}", e.getMessage());
            throw new RuntimeException("Failed to compute CMI signature", e);
        }
    }

    public String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] raw = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (NoSuchAlgorithmException e) {
            log.error("[CMI] Failed to compute SHA-256: {}", e.getMessage());
            throw new RuntimeException("Failed to compute CMI SHA-256 signature", e);
        }
    }

    public boolean verifySignature(String data, String signature, String secretKey) {
        String expected = computeSignature(data, secretKey);
        return constantTimeEquals(expected, signature);
    }

    public String formatAmount(java.math.BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    public String buildSignatureData(String... values) {
        return Arrays.stream(values)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining("|"));
    }

    public String buildSignatureDataWithEmpty(String... values) {
        return Arrays.stream(values)
                .map(v -> v != null ? v : "")
                .collect(Collectors.joining("|"));
    }

    public String buildCallbackSignatureData(
            String totalAmount, String currency, String merchantCode,
            String store, String operator, String channel,
            String tokenRef, String orderId, String paymentMode,
            String fatouratiTransactionNumber, String storeApiKey) {
        return buildSignatureData(
                totalAmount,
                currency,
                merchantCode,
                store,
                operator,
                channel,
                tokenRef,
                orderId,
                paymentMode,
                fatouratiTransactionNumber,
                storeApiKey
        );
    }

    public String buildCancelSignatureData(
            String totalAmount, String currency, String merchantCode,
            String store, String token, String orderId,
            String fatouratiTransactionNumber, String storeApiKey) {
        return buildSignatureData(
                totalAmount,
                currency,
                merchantCode,
                store,
                token,
                orderId,
                fatouratiTransactionNumber,
                storeApiKey
        );
    }

    public String buildTokenGenSignatureData(
            String totalAmount, String currency, String merchantCode,
            String store, String orderId, String cashierId,
            String paymentMode, String isCancel, String expiryDate, String storeApiKey) {
        return buildSignatureData(
                totalAmount,
                currency,
                merchantCode,
                store,
                orderId,
                cashierId,
                paymentMode,
                isCancel,
                expiryDate,
                storeApiKey
        );
    }

    public boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
