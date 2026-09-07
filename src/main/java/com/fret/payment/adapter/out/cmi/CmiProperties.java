package com.fret.payment.adapter.out.cmi;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.fatourati")
public class CmiProperties {

    private String baseUrl = "https://agg-merchant-qa.cmi.co.ma";
    private String authUrl = "https://auth-dev.cmi.co.ma";
    private String authRealm = "pay-gate-ext-qa";
    private String cashierId = "001";
    private String apiVersion = "v1";
    private String clientId;
    private String clientSecret;
    private String merchantCode;
    private String store;
    private String storeApiKey;
    private String callbackUrl;
    private String cancelUrl;
    private String checkStatusUrl;
    private String callbackSecret;
    private String signatureAlgorithm = "HMAC-SHA256";
    private String clientName = "Client Nador West Med";
    private String clientEmail = "client.test@example.com";
    private String clientPhone = "+212600000000";
    private int tokenTtlMinutes = 60;
    private int connectionTimeoutSeconds = 10;
    private int readTimeoutSeconds = 30;
}
