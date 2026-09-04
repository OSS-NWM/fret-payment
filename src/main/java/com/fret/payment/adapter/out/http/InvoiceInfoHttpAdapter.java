package com.fret.payment.adapter.out.http;

import com.fret.payment.adapter.out.cmi.CmiProperties;
import com.fret.payment.domain.model.payment.InvoiceInfo;
import com.fret.payment.domain.port.out.payment.InvoiceInfoPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceInfoHttpAdapter implements InvoiceInfoPort {

    private final CmiProperties cmiProperties;
    private final RestTemplate restTemplate;

    private static final String FRET_MANAGEMENT_URL = "http://localhost:8081";

    @Override
    @SuppressWarnings("unchecked")
    public List<InvoiceInfo> findByMouvementId(String mouvementId) {
        try {
            String url = FRET_MANAGEMENT_URL + "/api/invoices/mouvement/" + mouvementId;
            Object response = restTemplate.getForObject(url, Object.class);

            List<InvoiceInfo> result = new ArrayList<>();
            if (response instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) item;
                        InvoiceInfo info = InvoiceInfo.builder()
                                .id(map.get("id") != null ? map.get("id").toString() : null)
                                .mouvementId(mouvementId)
                                .montantTtc(parseBigDecimal(map.get("montantTtc")))
                                .build();
                        result.add(info);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[INVOICE_CLIENT] Failed to fetch invoices for mouvementId={}: {}", mouvementId, e.getMessage());
            return List.of();
        }
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
