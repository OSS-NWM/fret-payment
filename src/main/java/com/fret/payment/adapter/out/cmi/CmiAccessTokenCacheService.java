package com.fret.payment.adapter.out.cmi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class CmiAccessTokenCacheService {

    private final CmiProperties cmiProperties;
    private final CmiFatouratiClientAdapter cmiClient;

    public CmiAccessTokenCacheService(CmiProperties cmiProperties, @Lazy CmiFatouratiClientAdapter cmiClient) {
        this.cmiProperties = cmiProperties;
        this.cmiClient = cmiClient;
    }

    private record CachedToken(String accessToken, Instant expiresAt) {}

    private final AtomicReference<CachedToken> cachedToken = new AtomicReference<>();
    private final ConcurrentHashMap<String, String> errorLog = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("[CMI_TOKEN] Initializing access token cache");
    }

    public String getAccessToken() {
        CachedToken current = cachedToken.get();
        if (current != null && Instant.now().isBefore(current.expiresAt())) {
            return current.accessToken();
        }

        synchronized (this) {
            current = cachedToken.get();
            if (current != null && Instant.now().isBefore(current.expiresAt())) {
                return current.accessToken();
            }

            log.info("[CMI_TOKEN] Token missing or expired, requesting new token from CMI");
            return refreshToken();
        }
    }

    public String refreshToken() {
        try {
            String newToken = cmiClient.requestAccessToken();
            Instant expiresAt = Instant.now().plusSeconds(cmiProperties.getTokenTtlMinutes() * 60L - 300);
            cachedToken.set(new CachedToken(newToken, expiresAt));
            log.info("[CMI_TOKEN] Successfully obtained new access token, expires at {}", expiresAt);
            errorLog.clear();
            return newToken;
        } catch (Exception e) {
            log.error("[CMI_TOKEN] Failed to refresh access token: {}", e.getMessage());
            if (errorLog.size() < 5) {
                errorLog.put(Instant.now().toString(), e.getMessage());
            }
            throw new RuntimeException("CMI access token refresh failed", e);
        }
    }

    @Scheduled(fixedRate = 300000)
    public void proactivelyRefresh() {
        CachedToken current = cachedToken.get();
        if (current != null) {
            long remainingSeconds = current.expiresAt().getEpochSecond() - Instant.now().getEpochSecond();
            if (remainingSeconds < 600) {
                log.info("[CMI_TOKEN] Proactive refresh triggered, {}s remaining", remainingSeconds);
                try {
                    refreshToken();
                } catch (Exception e) {
                    log.warn("[CMI_TOKEN] Proactive refresh failed: {}", e.getMessage());
                }
            }
        }
    }

    public void invalidate() {
        cachedToken.set(null);
        log.info("[CMI_TOKEN] Cache invalidated");
    }
}
