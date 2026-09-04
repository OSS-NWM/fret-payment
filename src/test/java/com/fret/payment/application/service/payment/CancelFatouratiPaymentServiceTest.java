package com.fret.payment.application.service.payment;

import com.fret.payment.adapter.out.persistance.adapter.FatouratiTokenRepositoryAdapter;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelFatouratiPaymentServiceTest {

    @Mock
    private FatouratiTokenRepositoryAdapter tokenRepository;

    private CancelFatouratiPaymentService service;

    @BeforeEach
    void setUp() {
        service = new CancelFatouratiPaymentService(tokenRepository);
    }

    @Test
    void cancel_activeTokenFound_marksCancelledLocally() {
        String mouvementId = "MV-123";
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef("TOKEN123")
                .mouvementId(mouvementId)
                .status(FatouratiTokenStatus.CREATED)
                .build();

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.of(token));

        service.cancel(mouvementId);

        verify(tokenRepository).updateStatus("TOKEN123", FatouratiTokenStatus.CANCELLED);
    }

    @Test
    void cancel_noActiveToken_noOp() {
        String mouvementId = "MV-UNKNOWN";

        when(tokenRepository.findActiveByMouvementId(mouvementId)).thenReturn(Optional.empty());

        service.cancel(mouvementId);

        verify(tokenRepository, never()).updateStatus(any(), any());
    }

    @Test
    void getTokenForStatus_returnsToken() {
        String tokenRef = "TOKEN123";
        FatouratiToken token = FatouratiToken.builder()
                .tokenRef(tokenRef)
                .status(FatouratiTokenStatus.CANCELLED)
                .build();

        when(tokenRepository.findByTokenRef(tokenRef)).thenReturn(Optional.of(token));

        Optional<FatouratiToken> result = service.getTokenForStatus(tokenRef);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(FatouratiTokenStatus.CANCELLED);
    }

    @Test
    void getTokenForStatus_unknownRef_returnsEmpty() {
        when(tokenRepository.findByTokenRef("UNKNOWN")).thenReturn(Optional.empty());

        Optional<FatouratiToken> result = service.getTokenForStatus("UNKNOWN");

        assertThat(result).isEmpty();
    }
}
