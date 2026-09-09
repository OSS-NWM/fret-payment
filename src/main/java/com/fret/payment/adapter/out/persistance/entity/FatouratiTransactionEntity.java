package com.fret.payment.adapter.out.persistance.entity;

import com.fret.payment.domain.model.payment.FatouratiTransaction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "fatourati_transaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FatouratiTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_ref", nullable = false, length = 64)
    private String tokenRef;

    @Column(name = "aggregator_code", length = 64)
    private String aggregatorCode;

    @Column(name = "channel", length = 64)
    private String channel;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "terminal_id", length = 64)
    private String terminalId;

    @Column(name = "fatourati_transaction_number", length = 100)
    private String fatouratiTransactionNumber;

    @Column(name = "payment_system_transaction_number", length = 100)
    private String paymentSystemTransactionNumber;

    @Column(name = "payment_mode", length = 50)
    private String paymentMode;

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 8)
    private String currency;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;

    @Column(name = "status", length = 50)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_items", columnDefinition = "jsonb")
    private List<FatouratiTransaction.SelectedItem> selectedItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public static FatouratiTransactionEntity fromDomain(FatouratiTransaction domain) {
        return FatouratiTransactionEntity.builder()
                .id(domain.getId())
                .tokenRef(domain.getTokenRef())
                .aggregatorCode(domain.getAggregatorCode())
                .channel(domain.getChannel())
                .operator(domain.getOperator())
                .terminalId(domain.getTerminalId())
                .fatouratiTransactionNumber(domain.getFatouratiTransactionNumber())
                .paymentSystemTransactionNumber(domain.getPaymentSystemTransactionNumber())
                .paymentMode(domain.getPaymentMode())
                .amount(domain.getAmount())
                .currency(domain.getCurrency())
                .transactionDate(domain.getTransactionDate())
                .receiptNumber(domain.getReceiptNumber())
                .status(domain.getStatus())
                .selectedItems(domain.getSelectedItems())
                .rawPayload(domain.getRawPayload())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    public FatouratiTransaction toDomain() {
        return FatouratiTransaction.builder()
                .id(id)
                .tokenRef(tokenRef)
                .aggregatorCode(aggregatorCode)
                .channel(channel)
                .operator(operator)
                .terminalId(terminalId)
                .fatouratiTransactionNumber(fatouratiTransactionNumber)
                .paymentSystemTransactionNumber(paymentSystemTransactionNumber)
                .paymentMode(paymentMode)
                .amount(amount)
                .currency(currency)
                .transactionDate(transactionDate)
                .receiptNumber(receiptNumber)
                .status(status)
                .selectedItems(selectedItems)
                .rawPayload(rawPayload)
                .createdAt(createdAt)
                .build();
    }
}
