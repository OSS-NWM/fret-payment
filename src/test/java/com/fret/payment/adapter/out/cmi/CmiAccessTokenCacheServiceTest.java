package com.fret.payment.adapter.out.cmi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CmiAccessTokenCacheServiceTest {

    @Mock
    private CmiFatouratiClientAdapter cmiClient;

    private CmiProperties cmiProperties;
    private CmiAccessTokenCacheService cacheService;

    @BeforeEach
    void setUp() {
        cmiProperties = new CmiProperties();
        cmiProperties.setTokenTtlMinutes(60);
        cacheService = new CmiAccessTokenCacheService(cmiProperties, cmiClient);
    }

    @Test
    void getAccessToken_firstCall_fetchesFromClient() {
        when(cmiClient.requestAccessToken()).thenReturn("token-abc123");

        String token = cacheService.getAccessToken();

        assertThat(token).isEqualTo("token-abc123");
        verify(cmiClient, times(1)).requestAccessToken();
    }

    @Test
    void getAccessToken_secondCallWithinTtl_reusesCachedToken() {
        when(cmiClient.requestAccessToken()).thenReturn("token-abc123");

        String token1 = cacheService.getAccessToken();
        String token2 = cacheService.getAccessToken();

        assertThat(token1).isEqualTo(token2);
        verify(cmiClient, times(1)).requestAccessToken();
    }

    @Test
    void getAccessToken_afterExpiry_fetchesNewToken() {
        when(cmiClient.requestAccessToken())
                .thenReturn("token-first")
                .thenReturn("token-second");

        String token1 = cacheService.getAccessToken();
        String token2 = cacheService.getAccessToken();

        assertThat(token1).isEqualTo("token-first");
        assertThat(token2).isEqualTo("token-first");
    }

    @Test
    void getAccessToken_afterInvalidate_fetchesNewToken() {
        when(cmiClient.requestAccessToken())
                .thenReturn("token-first")
                .thenReturn("token-second");

        String token1 = cacheService.getAccessToken();
        cacheService.invalidate();
        String token2 = cacheService.getAccessToken();

        assertThat(token1).isEqualTo("token-first");
        assertThat(token2).isEqualTo("token-second");
        verify(cmiClient, times(2)).requestAccessToken();
    }

    @Test
    void getAccessToken_clientThrows_propagatesException() {
        when(cmiClient.requestAccessToken())
                .thenThrow(new RuntimeException("Network error"));

        assertThatThrownBy(() -> cacheService.getAccessToken())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("CMI access token refresh failed");
    }

    @Test
    void getAccessToken_concurrentCalls_fetchesOnce() {
        when(cmiClient.requestAccessToken()).thenAnswer(inv -> {
            Thread.sleep(50);
            return "token-concurrent";
        });

        String token = cacheService.getAccessToken();
        assertThat(token).isEqualTo("token-concurrent");
        verify(cmiClient, times(1)).requestAccessToken();
    }

    @Test
    void refreshToken_success_updatesCache() {
        when(cmiClient.requestAccessToken()).thenReturn("token-refreshed");

        String token = cacheService.refreshToken();

        assertThat(token).isEqualTo("token-refreshed");
        String token2 = cacheService.getAccessToken();
        assertThat(token2).isEqualTo("token-refreshed");
        verify(cmiClient, times(1)).requestAccessToken();
    }

    @Test
    void invalidate_clearsCache() {
        when(cmiClient.requestAccessToken())
                .thenReturn("token-first")
                .thenReturn("token-second");

        cacheService.getAccessToken();
        cacheService.invalidate();

        String token2 = cacheService.getAccessToken();
        assertThat(token2).isEqualTo("token-second");
    }
}
