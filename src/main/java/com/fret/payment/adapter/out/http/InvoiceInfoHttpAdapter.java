package com.fret.payment.adapter.out.http;

import com.fret.payment.domain.model.payment.InvoiceInfo;
import com.fret.payment.domain.port.out.payment.InvoiceInfoPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class InvoiceInfoHttpAdapter implements InvoiceInfoPort {

    private final RestTemplate restTemplate;
    private final String fretManagementBaseUrl;

    public InvoiceInfoHttpAdapter(RestTemplate restTemplate,
                                  @Value("${app.fret-management.base-url:http://localhost:8081}") String fretManagementBaseUrl) {
        this.restTemplate = restTemplate;
        this.fretManagementBaseUrl = fretManagementBaseUrl;
    }

    @Override
    @SuppressWarnings("unchecked")
    public InvoiceInfo findById(Long invoiceId) {
        try {
            String url = fretManagementBaseUrl + "/api/internal/invoices/" + invoiceId;
            Object response = restTemplate.getForObject(url, Object.class);
            if (response instanceof Map) {
                return toInvoiceInfo((Map<String, Object>) response);
            }
            return null;
        } catch (Exception e) {
            log.warn("[INVOICE_CLIENT] Failed to fetch invoice id={}: {}", invoiceId, e.getMessage());
            return null;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<InvoiceInfo> findByIds(List<Long> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return List.of();
        }
        try {
            String url = fretManagementBaseUrl + "/api/internal/invoices?ids=" + String.join(",",
                    invoiceIds.stream().map(String::valueOf).toList());
            Object response = restTemplate.getForObject(url, Object.class);
            List<InvoiceInfo> result = new ArrayList<>();
            if (response instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        result.add(toInvoiceInfo((Map<String, Object>) item));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[INVOICE_CLIENT] Failed to fetch invoices by ids={}: {}", invoiceIds, e.getMessage());
            return List.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<InvoiceInfo> findByMouvementId(String mouvementId) {
        try {
            String url = fretManagementBaseUrl + "/api/internal/invoices/mouvement/" + mouvementId;
            Object response = restTemplate.getForObject(url, Object.class);

            List<InvoiceInfo> result = new ArrayList<>();
            if (response instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        result.add(toInvoiceInfo((Map<String, Object>) item));
                    }
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("[INVOICE_CLIENT] Failed to fetch invoices for mouvementId={}: {}", mouvementId, e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private InvoiceInfo toInvoiceInfo(Map<String, Object> map) {
        List<InvoiceInfo.InvoiceLineInfo> lignes = new ArrayList<>();
        Object lignesObj = map.get("lignes");
        if (lignesObj instanceof List<?> lignesList) {
            for (Object lineObj : lignesList) {
                if (lineObj instanceof Map lineMap) {
                    lignes.add(InvoiceInfo.InvoiceLineInfo.builder()
                            .idLine(parseLong(lineMap.get("idLine")))
                            .codeArticle((String) lineMap.get("codeArticle"))
                            .designation((String) lineMap.get("designation"))
                            .unite((String) lineMap.get("unite"))
                            .qte(parseInteger(lineMap.get("qte")))
                            .prixUnitaire(parseBigDecimal(lineMap.get("prixUnitaire")))
                            .remise(parseBigDecimal(lineMap.get("remise")))
                            .montantHt(parseBigDecimal(lineMap.get("montantHt")))
                            .montantHtMad(parseBigDecimal(lineMap.get("montantHtMad")))
                            .dateOperation(parseLocalDateTime(lineMap.get("dateOperation")))
                            .build());
                }
            }
        }

        return InvoiceInfo.builder()
                .id(parseLong(map.get("idHeader")))
                .numeroPiece((String) map.get("numeroPiece"))
                .mouvementId((String) map.get("mouvementId"))
                .montantTtc(parseBigDecimal(map.get("montantTtc")))
                .devise((String) map.get("devise"))
                .typePrestation((String) map.get("typePrestation"))
                .statutPaiement((String) map.get("statutPaiement"))
                .lignes(lignes)
                .build();
    }

    private Long parseLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
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

    private LocalDateTime parseLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
