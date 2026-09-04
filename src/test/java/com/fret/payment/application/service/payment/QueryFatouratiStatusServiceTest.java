package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.cmi.CmiFatouratiClientAdapter;
import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueryFatouratiStatusServiceTest {

    @Mock
    private FatouratiTokenRepositoryAdapter tokenRepository;
    @Mock
    private CmiFatouratiClientAdapter cmiClient;

    private QueryFatouratiStatusService service;

    @BeforeEach
    void setUp() {
        service = new QueryFatouratiStatusService(tokenRepository, cmiClient);
    }

    @Test
    void getStatus_tokenConsumed_returnsPaid() {
        String mouvementId = "MV-123";
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId(mouvementId)
                .status(FatouratiTokenStatus.CONSUMED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.of(token));

        FatouratiTransactionStatus result = service.getStatus(mouvementId);

        assertThat(result).isEqualTo(FatouratiTransactionStatus.PAID);
    }

    @Test
    void getStatus_tokenCancelled_returnsCancelled() {
        String mouvementId = "MV-123";
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId(mouvementId)
                .status(FatouratiTokenStatus.CANCELLED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.of(token));

        FatouratiTransactionStatus result = service.getStatus(mouvementId);

        assertThat(result).isEqualTo(FatouratiTransactionStatus.CANCELLED);
    }

    @Test
    void getStatus_tokenNotFound_returnsNotFound() {
        when(tokenRepository.findActiveByMouvementId("MV-UNKNOWN")).thenReturn(Optional.empty());

        FatouratiTransactionStatus result = service.getStatus("MV-UNKNOWN");

        assertThat(result).isEqualTo(FatouratiTransactionStatus.NOT_FOUND);
    }

    @Test
    void getStatus_tokenCreated_pending() {
        String mouvementId = "MV-123";
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.of(token));

        FatouratiTransactionStatus result = service.getStatus(mouvementId);

        assertThat(result).isEqualTo(FatouratiTransactionStatus.PENDING);
    }

    @Test
    void getStatus_tokenExpired_returnsNotFound() {
        String mouvementId = "MV-123";
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.of(token));

        FatouratiTransactionStatus result = service.getStatus(mouvementId);

        assertThat(result).isEqualTo(FatouratiTransactionStatus.NOT_FOUND);
    }

    @Test
    void getToken_returnsFromRepository() {
        String mouvementId = "MV-123";
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId(mouvementId)
                .totalAmount(new BigDecimal("150.00"))
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.of(token));

        FatouratiToken result = service.getToken(mouvementId);

        assertThat(result).isNotNull();
        assertThat(result.getTokenRef()).isEqualTo("TOKEN123");
    }

    @Test
    void getToken_notFound_returnsNull() {
        when(tokenRepository.findActiveByMouvementId("MV-UNKNOWN")).thenReturn(Optional.empty());

        FatouratiToken result = service.getToken("MV-UNKNOWN");

        assertThat(result).isNull();
    }
}
