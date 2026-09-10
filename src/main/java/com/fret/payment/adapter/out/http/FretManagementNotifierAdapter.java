package com.fret.payment.adapter.out.http;

import com.fret.payment.domain.port.out.FretManagementNotifierPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class FretManagementNotifierAdapter implements FretManagementNotifierPort {

    private final RestTemplate restTemplate;
    private final String fretManagementBaseUrl;

    public FretManagementNotifierAdapter(RestTemplate restTemplate,
                                         @Value("${app.fret-management.base-url:http://localhost:8081}") String fretManagementBaseUrl) {
        this.restTemplate = restTemplate;
        this.fretManagementBaseUrl = fretManagementBaseUrl;
    }

    @Override
    public void notifyPaymentConfirmed(String tokenRef, String mouvementId, BigDecimal amount,
                                       String currency, String transactionNumber) {
        String url = fretManagementBaseUrl + "/api/internal/fatourati/payment-confirmed";

        FatouratiPaymentConfirmedNotificationDto payload = FatouratiPaymentConfirmedNotificationDto.builder()
                .tokenRef(tokenRef)
                .mouvementId(mouvementId)
                .totalAmount(amount)
                .currency(currency)
                .transactionNumber(transactionNumber)
                .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();

        try {
            restTemplate.postForEntity(url, payload, Void.class);
            log.info("[NOTIFIER] Payment confirmed notification sent: tokenRef={}, mouvementId={}", tokenRef, mouvementId);
        } catch (Exception e) {
            log.warn("[NOTIFIER] Failed to notify fret-management of payment confirmation: tokenRef={}, erreur={}",
                    tokenRef, e.getMessage());
        }
    }

    @Override
    public void notifyPaymentConfirmedByInvoiceId(String tokenRef, Long invoiceId, String mouvementId,
                                                  BigDecimal amount, String currency, String transactionNumber,
                                                  String channel, String operator, String aggregatorCode,
                                                  String paymentSystemTransactionNumber) {
        String url = fretManagementBaseUrl + "/api/internal/fatourati/payment-confirmed";

        FatouratiPaymentConfirmedNotificationDto payload = FatouratiPaymentConfirmedNotificationDto.builder()
                .tokenRef(tokenRef)
                .invoiceId(invoiceId)
                .mouvementId(mouvementId)
                .totalAmount(amount)
                .currency(currency)
                .transactionNumber(transactionNumber)
                .transactionDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .channel(channel)
                .operator(operator)
                .aggregatorCode(aggregatorCode)
                .paymentSystemTransactionNumber(paymentSystemTransactionNumber)
                .build();

        try {
            restTemplate.postForEntity(url, payload, Void.class);
            log.info("[NOTIFIER] Payment confirmed (by invoiceId) notification sent: tokenRef={}, invoiceId={}",
                    tokenRef, invoiceId);
        } catch (Exception e) {
            log.warn("[NOTIFIER] Failed to notify fret-management of payment confirmation by invoiceId: tokenRef={}, erreur={}",
                    tokenRef, e.getMessage());
        }
    }
}
