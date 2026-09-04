package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiFatouratiClientAdapter;
import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.InvoiceInfo;
import com.fret.payment.domain.port.out.payment.InvoiceInfoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiateFatouratiPaymentServiceTest {

    @Mock
    private CmiFatouratiClientAdapter cmiClient;
    @Mock
    private FatouratiTokenRepositoryAdapter tokenRepository;
    @Mock
    private CmiProperties cmiProperties;
    @Mock
    private InvoiceInfoPort invoiceInfoPort;

    private InitiateFatouratiPaymentService service;

    @BeforeEach
    void setUp() {
        service = new InitiateFatouratiPaymentService(cmiClient, tokenRepository, cmiProperties, invoiceInfoPort);
    }

    @Test
    void initiate_activeTokenExists_returnsExisting() {
        String mouvementId = "MV-123";
        FatouratiToken existing = FatouratiToken.builder()
                .tokenRef("EXISTING-TOKEN")
                .mouvementId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.of(existing));

        FatouratiToken result = service.initiate(mouvementId);

        assertThat(result.getTokenRef()).isEqualTo("EXISTING-TOKEN");
        verify(cmiClient, never()).generateToken(any(), any(), any(), any(), any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void initiate_noActiveToken_createsNew() {
        String mouvementId = "MV-456";
        InvoiceInfo invoice = InvoiceInfo.builder().montantTtc(new BigDecimal("150.00")).build();
        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("NEW-TOKEN")
                .orderId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findByMouvementId(mouvementId)).thenReturn(List.of(invoice));
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiClient.generateToken(eq(mouvementId), any(BigDecimal.class), any(), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FatouratiToken result = service.initiate(mouvementId);

        assertThat(result.getTokenRef()).isEqualTo("NEW-TOKEN");
        assertThat(result.getMouvementId()).isEqualTo(mouvementId);
        assertThat(result.getStatus()).isEqualTo(FatouratiTokenStatus.CREATED);
        verify(tokenRepository).save(any(FatouratiToken.class));
    }

    @Test
    void initiate_invoiceNotFound_usesDefaultAmount() {
        String mouvementId = "MV-789";
        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("NEW-TOKEN")
                .orderId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findByMouvementId(mouvementId)).thenReturn(List.of());
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiClient.generateToken(eq(mouvementId), any(BigDecimal.class), any(), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FatouratiToken result = service.initiate(mouvementId);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void initiate_invoiceZeroAmount_usesDefaultAmount() {
        String mouvementId = "MV-ZERO";
        InvoiceInfo invoice = InvoiceInfo.builder().montantTtc(BigDecimal.ZERO).build();
        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("NEW-TOKEN")
                .orderId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findByMouvementId(mouvementId)).thenReturn(List.of(invoice));
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiClient.generateToken(eq(mouvementId), any(BigDecimal.class), any(), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initiate(mouvementId);

        verify(cmiClient).generateToken(eq(mouvementId), any(BigDecimal.class), any(), any(), any(), any());
    }

    @Test
    void initiate_savesTokenWithCorrectAmount() {
        String mouvementId = "MV-AMOUNT";
        InvoiceInfo invoice = InvoiceInfo.builder().montantTtc(new BigDecimal("250.75")).build();
        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("TOKEN-AMOUNT")
                .orderId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findByMouvementId(mouvementId)).thenReturn(List.of(invoice));
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiClient.generateToken(eq(mouvementId), any(BigDecimal.class), any(), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FatouratiToken result = service.initiate(mouvementId);

        assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("250.75"));
    }
}
