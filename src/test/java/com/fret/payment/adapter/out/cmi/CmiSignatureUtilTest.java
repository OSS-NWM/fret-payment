package com.fret.payment.adapter.out.cmi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CmiSignatureUtilTest {

    private CmiSignatureUtil util;

    @BeforeEach
    void setUp() {
        util = new CmiSignatureUtil();
    }

    @Test
    void computeSignature_knownVector() {
        String data = "hello";
        String secret = "secretkey";
        String sig = util.computeSignature(data, secret);
        assertThat(sig).hasSize(64);
        assertThat(sig).matches("[a-f0-9]{64}");
    }

    @Test
    void computeSignature_emptyData_produces64CharHex() {
        String sig = util.computeSignature("", "key");
        assertThat(sig).hasSize(64);
        assertThat(sig).matches("[a-f0-9]{64}");
    }

    @Test
    void computeSignature_differentData_producesDifferentSignature() {
        String sig1 = util.computeSignature("data1", "key");
        String sig2 = util.computeSignature("data2", "key");
        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void computeSignature_differentKey_producesDifferentSignature() {
        String sig1 = util.computeSignature("data", "key1");
        String sig2 = util.computeSignature("data", "key2");
        assertThat(sig1).isNotEqualTo(sig2);
    }

    @Test
    void computeSignature_sameInputs_producesDeterministicOutput() {
        String sig1 = util.computeSignature("order123", "apikey");
        String sig2 = util.computeSignature("order123", "apikey");
        assertThat(sig1).isEqualTo(sig2);
    }

    @Test
    void computeSignature_containsLowercaseHexOnly() {
        String sig = util.computeSignature("any data", "anykey");
        assertThat(sig).matches("[a-f0-9]+");
        assertThat(sig).doesNotContainPattern("[A-F]");
    }

    @Test
    void verifySignature_validSignature_returnsTrue() {
        String data = "totalAmount|currency|merchantCode";
        String secret = "storeApiKey";
        String sig = util.computeSignature(data, secret);
        assertThat(util.verifySignature(data, sig, secret)).isTrue();
    }

    @Test
    void verifySignature_invalidSignature_returnsFalse() {
        String data = "totalAmount|currency|merchantCode";
        String secret = "storeApiKey";
        assertThat(util.verifySignature(data, "invalid_signature", secret)).isFalse();
    }

    @Test
    void verifySignature_tamperedData_returnsFalse() {
        String data = "totalAmount|currency|merchantCode";
        String secret = "storeApiKey";
        String sig = util.computeSignature(data, secret);
        assertThat(util.verifySignature("tamperedData", sig, secret)).isFalse();
    }

    @Test
    void formatAmount_integerValue_padsTwoDecimals() {
        assertThat(util.formatAmount(new BigDecimal("80"))).isEqualTo("80.00");
        assertThat(util.formatAmount(new BigDecimal("0"))).isEqualTo("0.00");
        assertThat(util.formatAmount(new BigDecimal("1234"))).isEqualTo("1234.00");
    }

    @Test
    void formatAmount_oneDecimal_padsOneZero() {
        assertThat(util.formatAmount(new BigDecimal("80.5"))).isEqualTo("80.50");
        assertThat(util.formatAmount(new BigDecimal("1.2"))).isEqualTo("1.20");
    }

    @Test
    void formatAmount_twoDecimals_unchanged() {
        assertThat(util.formatAmount(new BigDecimal("80.00"))).isEqualTo("80.00");
        assertThat(util.formatAmount(new BigDecimal("1234.56"))).isEqualTo("1234.56");
    }

    @Test
    void formatAmount_largeAmount_preservesDigits() {
        assertThat(util.formatAmount(new BigDecimal("999999.99"))).isEqualTo("999999.99");
    }

    @Test
    void formatAmount_smallAmount_preservesZeros() {
        assertThat(util.formatAmount(new BigDecimal("0.01"))).isEqualTo("0.01");
        assertThat(util.formatAmount(new BigDecimal("0.10"))).isEqualTo("0.10");
    }

    @Test
    void buildSignatureData_allNonNull_joinsWithPipe() {
        String result = util.buildSignatureData("100.00", "504", "100024");
        assertThat(result).isEqualTo("100.00|504|100024");
    }

    @Test
    void buildSignatureData_nullValues_skipped() {
        String result = util.buildSignatureData("100.00", null, "504");
        assertThat(result).isEqualTo("100.00|504");
    }

    @Test
    void buildSignatureData_blankValues_skipped() {
        String result = util.buildSignatureData("100.00", "", "504");
        assertThat(result).isEqualTo("100.00|504");
    }

    @Test
    void buildSignatureData_mixedNullAndBlank_skipsBoth() {
        String result = util.buildSignatureData("100.00", null, "", "504", null);
        assertThat(result).isEqualTo("100.00|504");
    }

    @Test
    void buildSignatureData_emptyArray_returnsEmptyString() {
        String result = util.buildSignatureData();
        assertThat(result).isEmpty();
    }

    @Test
    void buildSignatureData_singleValue_noPipe() {
        String result = util.buildSignatureData("only");
        assertThat(result).isEqualTo("only");
    }

    @Test
    void buildSignatureData_allNullOrBlank_returnsEmptyString() {
        String result = util.buildSignatureData(null, "", "   ");
        assertThat(result).isEmpty();
    }

    @Test
    void buildCallbackSignatureData_fullFields_joinsInOrder() {
        String result = util.buildCallbackSignatureData(
                "100.00", "504", "100024", "100030",
                "ORANGE_MAROC", "MOBILE_MONEY",
                "TOKEN123", "ORDER456", "CASH", "TRX789", "SECRETKEY");
        assertThat(result).isEqualTo(
                "100.00|504|100024|100030|ORANGE_MAROC|MOBILE_MONEY|TOKEN123|ORDER456|CASH|TRX789|SECRETKEY");
    }

    @Test
    void buildCallbackSignatureData_nullFields_skipped() {
        String result = util.buildCallbackSignatureData(
                "100.00", "504", null, null,
                null, null,
                "TOKEN123", null, null, null, null);
        assertThat(result).isEqualTo("100.00|504|TOKEN123");
    }

    @Test
    void buildCallbackSignatureData_onlyRequiredFields() {
        String result = util.buildCallbackSignatureData(
                "100.00", "504", "100024", "100030",
                null, null,
                "TOKEN123", "ORDER456", null, null, null);
        assertThat(result).isEqualTo("100.00|504|100024|100030|TOKEN123|ORDER456");
    }

    @Test
    void buildCancelSignatureData_fullFields_joinsInOrder() {
        String result = util.buildCancelSignatureData(
                "100.00", "504", "100024", "100030",
                "TOKEN123", "ORDER456", "TRX789", "SECRETKEY");
        assertThat(result).isEqualTo(
                "100.00|504|100024|100030|TOKEN123|ORDER456|TRX789|SECRETKEY");
    }

    @Test
    void buildCancelSignatureData_nullFields_skipped() {
        String result = util.buildCancelSignatureData(
                "100.00", "504", null, null,
                "TOKEN123", null, null, null);
        assertThat(result).isEqualTo("100.00|504|TOKEN123");
    }

    @Test
    void buildTokenGenSignatureData_fullFields_joinsInOrder() {
        String result = util.buildTokenGenSignatureData(
                "100.00", "504", "100024", "100030",
                "ORDER456", "001", "MULTI_CANAL", "0",
                "2025-12-31T23:59:00", "SECRETKEY");
        assertThat(result).isEqualTo(
                "100.00|504|100024|100030|ORDER456|001|MULTI_CANAL|0|2025-12-31T23:59:00|SECRETKEY");
    }

    @Test
    void buildTokenGenSignatureData_nullFields_skipped() {
        String result = util.buildTokenGenSignatureData(
                "100.00", null, null, null,
                "ORDER456", null, null, null,
                null, null);
        assertThat(result).isEqualTo("100.00|ORDER456");
    }

    @Test
    void buildSignatureDataWithEmpty_nullBecomesEmpty() {
        String result = util.buildSignatureDataWithEmpty("100.00", null, "504");
        assertThat(result).isEqualTo("100.00||504");
    }

    @Test
    void buildSignatureDataWithEmpty_noFilter() {
        String result = util.buildSignatureDataWithEmpty("100.00", "", "504");
        assertThat(result).isEqualTo("100.00||504");
    }

    @Test
    void constantTimeEquals_identicalStrings_returnsTrue() {
        assertThat(util.constantTimeEquals("abc123", "abc123")).isTrue();
    }

    @Test
    void constantTimeEquals_differentStrings_returnsFalse() {
        assertThat(util.constantTimeEquals("abc123", "abc124")).isFalse();
    }

    @Test
    void constantTimeEquals_nullFirst_returnsFalse() {
        assertThat(util.constantTimeEquals(null, "abc")).isFalse();
    }

    @Test
    void constantTimeEquals_nullSecond_returnsFalse() {
        assertThat(util.constantTimeEquals("abc", null)).isFalse();
    }

    @Test
    void constantTimeEquals_bothNull_returnsFalse() {
        assertThat(util.constantTimeEquals(null, null)).isFalse();
    }

    @Test
    void constantTimeEquals_differentLengths_returnsFalse() {
        assertThat(util.constantTimeEquals("short", "muchlonger")).isFalse();
        assertThat(util.constantTimeEquals("muchlonger", "short")).isFalse();
    }

    @Test
    void constantTimeEquals_emptyStrings_returnsTrue() {
        assertThat(util.constantTimeEquals("", "")).isTrue();
    }

    @Test
    void constantTimeEquals_emptyAndNonEmpty_returnsFalse() {
        assertThat(util.constantTimeEquals("", "abc")).isFalse();
        assertThat(util.constantTimeEquals("abc", "")).isFalse();
    }

    @Test
    void constantTimeEquals_caseSensitive() {
        assertThat(util.constantTimeEquals("ABC", "abc")).isFalse();
    }

    @Test
    void computeSignature_tokenGenSignatureData_roundTrips() {
        String totalAmount = "80.00";
        String currency = "504";
        String merchantCode = "100024";
        String store = "100030";
        String orderId = "MV-123";
        String cashierId = "001";
        String paymentMode = "MULTI_CANAL";
        String isCancel = "0";
        String expiryDate = "2025-12-31T23:59:00";
        String storeApiKey = "SECRETKEY";

        String signatureData = util.buildTokenGenSignatureData(
                totalAmount, currency, merchantCode, store,
                orderId, cashierId, paymentMode, isCancel, expiryDate, storeApiKey);

        String sig = util.computeSignature(signatureData, storeApiKey);

        assertThat(util.verifySignature(signatureData, sig, storeApiKey)).isTrue();
    }

    @Test
    void computeSignature_callbackSignatureData_roundTrips() {
        String totalAmount = "100.00";
        String currency = "504";
        String merchantCode = "100024";
        String store = "100030";
        String operator = "ORANGE_MAROC";
        String channel = "MOBILE_MONEY";
        String tokenRef = "TOKEN123";
        String orderId = "ORDER456";
        String paymentMode = "CASH";
        String fatouratiTransactionNumber = "TRX789";
        String storeApiKey = "SECRETKEY";

        String signatureData = util.buildCallbackSignatureData(
                totalAmount, currency, merchantCode, store,
                operator, channel, tokenRef, orderId, paymentMode,
                fatouratiTransactionNumber, storeApiKey);

        String sig = util.computeSignature(signatureData, storeApiKey);

        assertThat(util.verifySignature(signatureData, sig, storeApiKey)).isTrue();
    }

    @Test
    void computeSignature_cancelSignatureData_roundTrips() {
        String totalAmount = "100.00";
        String currency = "504";
        String merchantCode = "100024";
        String store = "100030";
        String token = "TOKEN123";
        String orderId = "ORDER456";
        String fatouratiTransactionNumber = "TRX789";
        String storeApiKey = "SECRETKEY";

        String signatureData = util.buildCancelSignatureData(
                totalAmount, currency, merchantCode, store,
                token, orderId, fatouratiTransactionNumber, storeApiKey);

        String sig = util.computeSignature(signatureData, storeApiKey);

        assertThat(util.verifySignature(signatureData, sig, storeApiKey)).isTrue();
    }
}
