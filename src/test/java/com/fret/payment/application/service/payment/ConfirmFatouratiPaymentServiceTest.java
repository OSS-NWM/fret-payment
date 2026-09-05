package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.cmi.CmiSignatureUtil;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiCallbackLogRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiPaymentCallback;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.port.out.FretManagementNotifierPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConfirmFatouratiPaymentServiceTest {

    @Mock
    private FatouratiTokenRepositoryAdapter tokenRepository;
    @Mock
    private FatouratiCallbackLogRepositoryAdapter callbackLogRepository;
    @Mock
    private CmiSignatureUtil signatureUtil;
    @Mock
    private CmiProperties cmiProperties;
    @Mock
    private FretManagementNotifierPort fretManagementNotifier;
    private ObjectMapper objectMapper;
    private ConfirmFatouratiPaymentService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new ConfirmFatouratiPaymentService(
                tokenRepository, callbackLogRepository, signatureUtil, cmiProperties, objectMapper, fretManagementNotifier);
    }

    @Test
    void confirmPayment_validSignature_marksTokenConsumed_andReturnsZero() {
        String tokenRef = "TOKEN123";
        String signature = "valid_sig_hex";
        String signatureData = "100.00|504|100024|100030|ORANGE_MAROC|MOBILE_MONEY|T TOKEN123|ORDER456|CASH|TRX789|SECRETKEY";

        FatouratiPaymentCallback callback = FatouratiPaymentCallback.builder()
                .tokenRef(tokenRef)
                .orderId("ORDER456")
                .totalAmount(new BigDecimal("100.00"))
                .currency("504")
                .operator("ORANGE_MAROC")
                .channel("MOBILE_MONEY")
                .paymentMode("CASH")
                .fatouratiTransactionNumber("TRX789")
                .decisionCode(0)
                .signature(signature)
                .build();

        FatouratiToken token = FatouratiToken.builder()
                .tokenRef(tokenRef)
                .mouvementId("MV-123")
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(callbackLogRepository.isCallbackProcessed("TRX789")).thenReturn(false);
        when(signatureUtil.formatAmount(any())).thenReturn("100.00");
        when(cmiProperties.getMerchantCode()).thenReturn("100024");
        when(cmiProperties.getStore()).thenReturn("100030");
        when(cmiProperties.getStoreApiKey()).thenReturn("SECRETKEY");
        when(signatureUtil.buildCallbackSignatureData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(signatureData);
        when(signatureUtil.computeSignature(signatureData, "SECRETKEY")).thenReturn(signature);
        when(signatureUtil.constantTimeEquals(signature, signature)).thenReturn(true);
        when(tokenRepository.findByTokenRef(tokenRef)).thenReturn(Optional.of(token));

        String result = service.confirmPayment(callback);

        assertThat(result).isEqualTo("0");
        verify(tokenRepository).updateStatus(tokenRef, FatouratiTokenStatus.CONSUMED);
    }

    @Test
    void confirmPayment_invalidSignature_returnsThree() {
        FatouratiPaymentCallback callback = FatouratiPaymentCallback.builder()
                .tokenRef("TOKEN123")
                .fatouratiTransactionNumber("TRX789")
                .signature("invalid_signature")
                .decisionCode(0)
                .build();

        when(callbackLogRepository.isCallbackProcessed("TRX789")).thenReturn(false);
        when(signatureUtil.formatAmount(any())).thenReturn("100.00");
        when(cmiProperties.getMerchantCode()).thenReturn("100024");
        when(cmiProperties.getStore()).thenReturn("100030");
        when(cmiProperties.getStoreApiKey()).thenReturn("SECRETKEY");
        when(signatureUtil.buildCallbackSignatureData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("some_data");
        when(signatureUtil.computeSignature(any(), eq("SECRETKEY"))).thenReturn("real_sig");
        when(signatureUtil.constantTimeEquals("real_sig", "invalid_signature")).thenReturn(false);

        String result = service.confirmPayment(callback);

        assertThat(result).isEqualTo("3");
        verify(tokenRepository, never()).updateStatus(any(), any());
    }

    @Test
    void confirmPayment_duplicateCallback_returnsTwo_idempotent() {
        String tokenRef = "TOKEN123";
        FatouratiPaymentCallback callback = FatouratiPaymentCallback.builder()
                .tokenRef(tokenRef)
                .fatouratiTransactionNumber("TRX789")
                .signature("sig")
                .decisionCode(0)
                .build();

        when(callbackLogRepository.isCallbackProcessed("TRX789")).thenReturn(true);

        String result = service.confirmPayment(callback);

        assertThat(result).isEqualTo("2");
        verify(tokenRepository, never()).updateStatus(any(), any());
    }

    @Test
    void confirmPayment_unknownToken_returnsThree() {
        FatouratiPaymentCallback callback = FatouratiPaymentCallback.builder()
                .tokenRef("UNKNOWN-TOKEN")
                .fatouratiTransactionNumber("TRX789")
                .signature("sig")
                .decisionCode(0)
                .build();

        when(callbackLogRepository.isCallbackProcessed("TRX789")).thenReturn(false);
        when(signatureUtil.formatAmount(any())).thenReturn("100.00");
        when(cmiProperties.getMerchantCode()).thenReturn("100024");
        when(cmiProperties.getStore()).thenReturn("100030");
        when(cmiProperties.getStoreApiKey()).thenReturn("SECRETKEY");
        when(signatureUtil.buildCallbackSignatureData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("data");
        when(signatureUtil.computeSignature(any(), eq("SECRETKEY"))).thenReturn("sig");
        when(signatureUtil.constantTimeEquals("sig", "sig")).thenReturn(true);
        when(tokenRepository.findByTokenRef("UNKNOWN-TOKEN")).thenReturn(Optional.empty());

        String result = service.confirmPayment(callback);

        assertThat(result).isEqualTo("3");
    }

    @Test
    void confirmPayment_noSignature_returnsFalse() {
        FatouratiPaymentCallback callback = FatouratiPaymentCallback.builder()
                .tokenRef("TOKEN123")
                .fatouratiTransactionNumber("TRX789")
                .signature(null)
                .decisionCode(0)
                .build();

        when(callbackLogRepository.isCallbackProcessed("TRX789")).thenReturn(false);

        String result = service.confirmPayment(callback);

        assertThat(result).isEqualTo("3");
        verify(tokenRepository, never()).updateStatus(any(), any());
    }

    @Test
    void confirmPayment_decisionCodeRefused_doesNotMarkConsumed() {
        String tokenRef = "TOKEN123";
        FatouratiPaymentCallback callback = FatouratiPaymentCallback.builder()
                .tokenRef(tokenRef)
                .fatouratiTransactionNumber("TRX789")
                .decisionCode(1)
                .signature("sig")
                .build();

        FatouratiToken token = FatouratiToken.builder()
                .tokenRef(tokenRef)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(callbackLogRepository.isCallbackProcessed("TRX789")).thenReturn(false);
        when(signatureUtil.formatAmount(any())).thenReturn("100.00");
        when(cmiProperties.getMerchantCode()).thenReturn("100024");
        when(cmiProperties.getStore()).thenReturn("100030");
        when(cmiProperties.getStoreApiKey()).thenReturn("SECRETKEY");
        when(signatureUtil.buildCallbackSignatureData(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("data");
        when(signatureUtil.computeSignature(any(), eq("SECRETKEY"))).thenReturn("sig");
        when(signatureUtil.constantTimeEquals("sig", "sig")).thenReturn(true);
        when(tokenRepository.findByTokenRef(tokenRef)).thenReturn(Optional.of(token));

        String result = service.confirmPayment(callback);

        assertThat(result).isEqualTo("0");
        verify(tokenRepository, never()).updateStatus(eq(tokenRef), eq(FatouratiTokenStatus.CONSUMED));
    }

    @Test
    void generateReceiptNumber_returnsRecPrefix() {
        String receipt = service.generateReceiptNumber();
        assertThat(receipt).startsWith("REC");
        assertThat(receipt).hasSize(3 + 13 + 4);
    }
}
