package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiFatouratiClientAdapter;
import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.adapter.out.cmi.InvoiceLinePayload;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.InvoiceInfo;
import com.fret.payment.domain.port.out.payment.InvoiceInfoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        Long invoiceId = 1001L;
        FatouratiToken existing = FatouratiToken.builder()
                .tokenRef("EXISTING-TOKEN")
                .invoiceId(invoiceId)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByInvoiceId(invoiceId)).thenReturn(Optional.of(existing));

        FatouratiToken result = service.initiate(invoiceId);

        assertThat(result.getTokenRef()).isEqualTo("EXISTING-TOKEN");
        verify(cmiClient, never()).generateToken(any(), any(), any(), any(), any(), any(), any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void initiate_noActiveToken_createsNew() {
        Long invoiceId = 1002L;
        InvoiceInfo invoice = InvoiceInfo.builder()
                .id(invoiceId)
                .numeroPiece("FAC-2026-00123")
                .mouvementId("MV-456")
                .montantTtc(new BigDecimal("150.00"))
                .devise("504")
                .lignes(List.of(
                        InvoiceInfo.InvoiceLineInfo.builder()
                                .idLine(1L)
                                .codeArticle("ART-001")
                                .designation("Droit de port")
                                .montantHt(new BigDecimal("150.00"))
                                .build()
                ))
                .build();
        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("NEW-TOKEN")
                .orderId("FAC-2026-00123")
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByInvoiceId(invoiceId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findById(invoiceId)).thenReturn(invoice);
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiProperties.getCancelUrl()).thenReturn("http://localhost:8081/cancel");
        when(cmiProperties.getCheckStatusUrl()).thenReturn("http://localhost:8081/status");
        when(cmiClient.generateToken(eq(invoiceId), eq("FAC-2026-00123"), any(), any(), eq("504"), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FatouratiToken result = service.initiate(invoiceId);

        assertThat(result.getTokenRef()).isEqualTo("NEW-TOKEN");
        assertThat(result.getInvoiceId()).isEqualTo(invoiceId);
        assertThat(result.getMouvementId()).isEqualTo("MV-456");
        assertThat(result.getStatus()).isEqualTo(FatouratiTokenStatus.CREATED);
        verify(tokenRepository).save(any(FatouratiToken.class));
    }

    @Test
    void initiate_invoiceNotFound_throws() {
        Long invoiceId = 9999L;
        when(tokenRepository.findActiveByInvoiceId(invoiceId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findById(invoiceId)).thenReturn(null);

        assertThatThrownBy(() -> service.initiate(invoiceId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invoice not found");
    }

    @Test
    void initiate_invoiceZeroAmount_usesDefault() {
        Long invoiceId = 1003L;
        InvoiceInfo invoice = InvoiceInfo.builder()
                .id(invoiceId)
                .numeroPiece("FAC-2026-00100")
                .mouvementId("MV-ZERO")
                .devise("504")
                .lignes(List.of(
                        InvoiceInfo.InvoiceLineInfo.builder()
                                .idLine(1L)
                                .codeArticle("ART-ZERO")
                                .designation("Article zero")
                                .montantHt(BigDecimal.ZERO)
                                .build()
                ))
                .build();
        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("ZERO-TOKEN")
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByInvoiceId(invoiceId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findById(invoiceId)).thenReturn(invoice);
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiProperties.getCancelUrl()).thenReturn("http://localhost:8081/cancel");
        when(cmiProperties.getCheckStatusUrl()).thenReturn("http://localhost:8081/status");
        when(cmiClient.generateToken(eq(invoiceId), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initiate(invoiceId);

        ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(cmiClient).generateToken(eq(invoiceId), any(), any(), amountCaptor.capture(), any(), any(), any(), any());
        assertThat(amountCaptor.getValue()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void initiate_buildsItemsFromInvoiceLines() {
        Long invoiceId = 1004L;
        InvoiceInfo invoice = InvoiceInfo.builder()
                .id(invoiceId)
                .numeroPiece("FAC-2026-00104")
                .mouvementId("MV-LINES")
                .devise("504")
                .lignes(List.of(
                        InvoiceInfo.InvoiceLineInfo.builder()
                                .idLine(10L)
                                .codeArticle("ART-001")
                                .designation("Droit de port")
                                .montantHt(new BigDecimal("100.00"))
                                .build(),
                        InvoiceInfo.InvoiceLineInfo.builder()
                                .idLine(11L)
                                .codeArticle("ART-002")
                                .designation("Manutention")
                                .montantHt(new BigDecimal("50.00"))
                                .build()
                ))
                .build();
        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("LINES-TOKEN")
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByInvoiceId(invoiceId)).thenReturn(Optional.empty());
        when(invoiceInfoPort.findById(invoiceId)).thenReturn(invoice);
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiProperties.getCancelUrl()).thenReturn("http://localhost:8081/cancel");
        when(cmiProperties.getCheckStatusUrl()).thenReturn("http://localhost:8081/status");
        when(cmiClient.generateToken(eq(invoiceId), eq("FAC-2026-00104"), any(), any(), eq("504"), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initiate(invoiceId);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InvoiceLinePayload>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cmiClient).generateToken(eq(invoiceId), eq("FAC-2026-00104"), itemsCaptor.capture(), any(), eq("504"), any(), any(), any());

        List<InvoiceLinePayload> capturedItems = itemsCaptor.getValue();
        assertThat(capturedItems).hasSize(2);
        assertThat(capturedItems.get(0).getIdLine()).isEqualTo(10L);
        assertThat(capturedItems.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedItems.get(1).getIdLine()).isEqualTo(11L);
        assertThat(capturedItems.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void initiateGroup_multipleInvoices_aggregatesItems() {
        List<Long> invoiceIds = List.of(2001L, 2002L);
        InvoiceInfo invoice1 = InvoiceInfo.builder()
                .id(2001L)
                .numeroPiece("FAC-2026-00201")
                .mouvementId("MV-G1")
                .devise("504")
                .lignes(List.of(
                        InvoiceInfo.InvoiceLineInfo.builder()
                                .idLine(20L)
                                .codeArticle("ART-001")
                                .designation("Ligne 1")
                                .montantHt(new BigDecimal("75.00"))
                                .build()
                ))
                .build();
        InvoiceInfo invoice2 = InvoiceInfo.builder()
                .id(2002L)
                .numeroPiece("FAC-2026-00202")
                .mouvementId("MV-G1")
                .devise("504")
                .lignes(List.of(
                        InvoiceInfo.InvoiceLineInfo.builder()
                                .idLine(21L)
                                .codeArticle("ART-002")
                                .designation("Ligne 2")
                                .montantHt(new BigDecimal("25.00"))
                                .build()
                ))
                .build();

        FatouratiToken createdToken = FatouratiToken.builder()
                .tokenRef("GROUP-TOKEN")
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(invoiceInfoPort.findById(2001L)).thenReturn(invoice1);
        when(invoiceInfoPort.findById(2002L)).thenReturn(invoice2);
        when(cmiProperties.getCallbackUrl()).thenReturn("http://localhost:8081/callback");
        when(cmiProperties.getCancelUrl()).thenReturn("http://localhost:8081/cancel");
        when(cmiProperties.getCheckStatusUrl()).thenReturn("http://localhost:8081/status");
        when(cmiClient.generateToken(eq(2001L), startsWith("GROUP-"), any(), any(), eq("504"), any(), any(), any()))
                .thenReturn(createdToken);
        when(tokenRepository.save(any(FatouratiToken.class))).thenAnswer(inv -> inv.getArgument(0));

        FatouratiToken result = service.initiateGroup(invoiceIds);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InvoiceLinePayload>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(cmiClient).generateToken(eq(2001L), startsWith("GROUP-"), itemsCaptor.capture(), any(), eq("504"), any(), any(), any());

        List<InvoiceLinePayload> capturedItems = itemsCaptor.getValue();
        assertThat(capturedItems).hasSize(2);
        assertThat(capturedItems.stream().map(InvoiceLinePayload::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
