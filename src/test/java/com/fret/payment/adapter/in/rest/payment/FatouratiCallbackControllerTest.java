package com.fret.payment.adapter.in.rest.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.cmi.CmiSignatureUtil;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenStatusHistoryRepositoryAdapter;
import com.fret.payment.application.service.payment.CancelFatouratiPaymentService;
import com.fret.payment.application.service.payment.ConfirmFatouratiPaymentService;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FatouratiCallbackControllerTest {

    @Mock
    private ConfirmFatouratiPaymentService confirmService;
    @Mock
    private CancelFatouratiPaymentService cancelService;
    @Mock
    private CmiSignatureUtil signatureUtil;
    @Mock
    private CmiProperties cmiProperties;
    @Mock
    private FatouratiTokenRepositoryAdapter tokenRepository;
    @Mock
    private FatouratiTokenStatusHistoryRepositoryAdapter historyRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        FatouratiCallbackController controller = new FatouratiCallbackController(
                confirmService, cancelService, signatureUtil, cmiProperties, objectMapper,
                tokenRepository, historyRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void callback_validSignature_returnsReceiptNumber() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","orderId":"MV-123","totalAmount":100.00,"currency":"504","decisionCode":0,"fatouratiTransactionNumber":"TRX789"}
                """;

        when(confirmService.confirmPayment(any())).thenReturn("REC1234567890ABCD");

        mockMvc.perform(post("/api/payment/fatourati/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "valid_sig")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("REC1234567890ABCD"));

        verify(confirmService).confirmPayment(any());
    }

    @Test
    void callback_invalidSignature_returns400() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","decisionCode":0,"fatouratiTransactionNumber":"TRX789"}
                """;

        when(confirmService.confirmPayment(any())).thenReturn("3");

        mockMvc.perform(post("/api/payment/fatourati/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "bad_sig")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SIGNATURE_INVALID"));
    }

    @Test
    void callback_malformedBody_returns400() throws Exception {
        String malformedJson = "{invalid json}";

        mockMvc.perform(post("/api/payment/fatourati/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "sig")
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BODY"));
    }

    @Test
    void callback_duplicate_returnsAlreadyProcessed() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","decisionCode":0,"fatouratiTransactionNumber":"TRX789"}
                """;

        when(confirmService.confirmPayment(any())).thenReturn("2");

        mockMvc.perform(post("/api/payment/fatourati/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "valid_sig")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("2"));
    }

    @Test
    void checkStatus_tokenPaid_returnsPaid() throws Exception {
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .status(FatouratiTokenStatus.CONSUMED)
                .build();

        when(cancelService.getTokenForStatus("TOKEN123")).thenReturn(Optional.of(token));

        mockMvc.perform(get("/api/payment/fatourati/check-status")
                        .param("token_ref", "TOKEN123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void checkStatus_tokenCancelled_returnsCancelled() throws Exception {
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .status(FatouratiTokenStatus.CANCELLED)
                .build();

        when(cancelService.getTokenForStatus("TOKEN123")).thenReturn(Optional.of(token));

        mockMvc.perform(get("/api/payment/fatourati/check-status")
                        .param("token_ref", "TOKEN123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void checkStatus_tokenNotFound_returnsNotFound() throws Exception {
        when(cancelService.getTokenForStatus("UNKNOWN")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payment/fatourati/check-status")
                        .param("token_ref", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void checkStatus_tokenExpired_returnsExpired() throws Exception {
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .status(FatouratiTokenStatus.EXPIRED)
                .build();

        when(cancelService.getTokenForStatus("TOKEN123")).thenReturn(Optional.of(token));

        mockMvc.perform(get("/api/payment/fatourati/check-status")
                        .param("token_ref", "TOKEN123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXPIRED"));
    }

    @Test
    void checkStatus_onlyTokenRefParam_returnsStatus() throws Exception {
        when(cancelService.getTokenForStatus("TOKEN123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payment/fatourati/check-status")
                        .param("token_ref", "TOKEN123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists());
    }

    @Test
    void cancel_validSignature_returnsOk() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","orderId":"MV-123","totalAmount":100.00,"currency":"504"}
                """;

        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId("MV-123")
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findByOrderId("MV-123")).thenReturn(Optional.of(token));
        when(cmiProperties.getMerchantCode()).thenReturn("100024");
        when(cmiProperties.getStore()).thenReturn("100030");
        when(cmiProperties.getStoreApiKey()).thenReturn("SECRETKEY");
        when(cmiProperties.getSignatureAlgorithm()).thenReturn("HmacSHA256");
        when(signatureUtil.buildCancelSignatureData(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("data");
        when(signatureUtil.computeSignature(eq("data"), any(), eq("SECRETKEY"))).thenReturn("valid_sig");
        when(signatureUtil.formatAmount(any())).thenReturn("100.00");
        when(signatureUtil.constantTimeEquals("valid_sig", "valid_sig")).thenReturn(true);

        mockMvc.perform(post("/api/payment/fatourati/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "valid_sig")
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(cancelService).cancel("MV-123");
    }

    @Test
    void cancel_invalidSignature_returns400() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","orderId":"MV-123","totalAmount":100.00,"currency":"504"}
                """;

        when(cmiProperties.getMerchantCode()).thenReturn("100024");
        when(cmiProperties.getStore()).thenReturn("100030");
        when(cmiProperties.getStoreApiKey()).thenReturn("SECRETKEY");
        when(signatureUtil.buildCancelSignatureData(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("data");
        when(signatureUtil.computeSignature("data", "SECRETKEY")).thenReturn("valid_sig");
        when(signatureUtil.formatAmount(any())).thenReturn("100.00");
        when(signatureUtil.constantTimeEquals("valid_sig", "bad_sig")).thenReturn(false);

        mockMvc.perform(post("/api/payment/fatourati/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "bad_sig")
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SIGNATURE_INVALID"));

        verify(cancelService, never()).cancel(any());
    }

    @Test
    void cancel_missingSignature_returns400() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","orderId":"MV-123","totalAmount":100.00,"currency":"504"}
                """;

        mockMvc.perform(post("/api/payment/fatourati/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_SIGNATURE"));

        verify(cancelService, never()).cancel(any());
    }

    @Test
    void callbackEndpoint_isPublic_noAuthRequired() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","orderId":"MV-123","totalAmount":100.00,"currency":"504","decisionCode":0,"fatouratiTransactionNumber":"TRX789"}
                """;

        when(confirmService.confirmPayment(any())).thenReturn("REC1234567890ABCD");

        mockMvc.perform(post("/api/payment/fatourati/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "valid_sig")
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.receiptNumber").value("REC1234567890ABCD"));
    }

    @Test
    void checkStatusEndpoint_isPublic_noAuthRequired() throws Exception {
        when(cancelService.getTokenForStatus("TOKEN123")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/payment/fatourati/check-status")
                        .param("token_ref", "TOKEN123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }

    @Test
    void cancelEndpoint_isPublic_noAuthRequired() throws Exception {
        String requestBody = """
                {"tokenRef":"TOKEN123","orderId":"MV-123","totalAmount":100.00,"currency":"504"}
                """;

        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId("MV-123")
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findByOrderId("MV-123")).thenReturn(Optional.of(token));
        when(signatureUtil.formatAmount(any())).thenReturn("100.00");
        when(cmiProperties.getMerchantCode()).thenReturn("100024");
        when(cmiProperties.getStore()).thenReturn("100030");
        when(cmiProperties.getStoreApiKey()).thenReturn("SECRETKEY");
        when(cmiProperties.getSignatureAlgorithm()).thenReturn("HmacSHA256");
        when(signatureUtil.buildCancelSignatureData(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn("data");
        when(signatureUtil.computeSignature(eq("data"), any(), eq("SECRETKEY"))).thenReturn("valid_sig");
        when(signatureUtil.constantTimeEquals("valid_sig", "valid_sig")).thenReturn(true);

        mockMvc.perform(post("/api/payment/fatourati/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-signature", "valid_sig")
                        .content(requestBody))
                .andExpect(status().isOk());

        verify(cancelService).cancel("MV-123");
    }
}
