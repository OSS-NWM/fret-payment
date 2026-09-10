package com.fret.payment.adapter.out.persistance.adapter;

import com.fret.payment.adapter.out.persistance.entity.FatouratiTokenEntity;
import com.fret.payment.adapter.out.persistance.entity.FatouratiTokenStatusHistoryEntity;
import com.fret.payment.adapter.out.persistance.repository.FatouratiTokenJpaRepository;
import com.fret.payment.adapter.out.persistance.repository.FatouratiTokenStatusHistoryJpaRepository;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.port.out.payment.FatouratiTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FatouratiTokenRepositoryAdapter implements FatouratiTokenRepositoryPort {

    private final FatouratiTokenJpaRepository jpaRepository;
    private final FatouratiTokenStatusHistoryJpaRepository historyJpaRepository;

    @Override
    public FatouratiToken save(FatouratiToken token) {
        FatouratiTokenEntity entity = toEntity(token);
        entity = jpaRepository.save(entity);
        return toDomain(entity);
    }

    @Override
    public Optional<FatouratiToken> findByTokenRef(String tokenRef) {
        return jpaRepository.findByTokenRef(tokenRef).map(this::toDomain);
    }

    @Override
    public Optional<FatouratiToken> findByMouvementId(String mouvementId) {
        return jpaRepository.findByMouvementId(mouvementId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void updateStatus(String tokenRef, FatouratiTokenStatus status) {
        jpaRepository.findByTokenRef(tokenRef).ifPresent(entity -> {
            FatouratiTokenStatus previousStatus = entity.getStatus();
            entity.setStatus(status);
            jpaRepository.save(entity);

            historyJpaRepository.save(FatouratiTokenStatusHistoryEntity.builder()
                    .tokenRef(tokenRef)
                    .previousStatus(previousStatus)
                    .newStatus(status)
                    .reason("STATUS_UPDATE")
                    .actor("SYSTEM")
                    .build());
        });
    }

    @Override
    @Transactional
    public void recordTransition(String tokenRef, FatouratiTokenStatus previousStatus,
                                 FatouratiTokenStatus newStatus, String reason, String actor,
                                 String channel, String operator) {
        jpaRepository.findByTokenRef(tokenRef).ifPresent(entity -> {
            entity.setStatus(newStatus);
            if (channel != null) entity.setPaymentChannel(channel);
            if (operator != null) entity.setPaymentOperator(operator);
            jpaRepository.save(entity);

            historyJpaRepository.save(FatouratiTokenStatusHistoryEntity.builder()
                    .tokenRef(tokenRef)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .reason(reason)
                    .actor(actor)
                    .channel(channel)
                    .operator(operator)
                    .build());
        });
    }

    @Override
    @Transactional
    public void updateConfirmation(String tokenRef, FatouratiTokenStatus newStatus,
                                   String channel, String operator, String reason, String actor) {
        jpaRepository.findByTokenRef(tokenRef).ifPresent(entity -> {
            FatouratiTokenStatus previousStatus = entity.getStatus();
            entity.setStatus(newStatus);
            if (channel != null) entity.setPaymentChannel(channel);
            if (operator != null) entity.setPaymentOperator(operator);
            jpaRepository.save(entity);

            historyJpaRepository.save(FatouratiTokenStatusHistoryEntity.builder()
                    .tokenRef(tokenRef)
                    .previousStatus(previousStatus)
                    .newStatus(newStatus)
                    .reason(reason)
                    .actor(actor)
                    .channel(channel)
                    .operator(operator)
                    .build());
        });
    }

    @Override
    public Optional<FatouratiToken> findActiveByMouvementId(String mouvementId) {
        return jpaRepository.findByMouvementIdAndStatus(mouvementId, FatouratiTokenStatus.CREATED)
                .map(this::toDomain);
    }

    @Override
    public Optional<FatouratiToken> findActiveByInvoiceId(Long invoiceId) {
        return jpaRepository.findByInvoiceIdAndStatus(invoiceId, FatouratiTokenStatus.CREATED)
                .map(this::toDomain);
    }

    @Override
    public Optional<FatouratiToken> findByInvoiceIdAndStatus(Long invoiceId, FatouratiTokenStatus status) {
        return jpaRepository.findByInvoiceIdAndStatus(invoiceId, status)
                .map(this::toDomain);
    }

    @Override
    public Optional<FatouratiToken> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId).map(this::toDomain);
    }

    private FatouratiTokenEntity toEntity(FatouratiToken token) {
        return FatouratiTokenEntity.builder()
                .id(token.getId())
                .tokenRef(token.getTokenRef())
                .invoiceId(token.getInvoiceId())
                .mouvementId(token.getMouvementId())
                .invoiceLineIds(token.getInvoiceLineIds())
                .orderId(token.getOrderId())
                .totalAmount(token.getTotalAmount())
                .currency(token.getCurrency())
                .status(token.getStatus())
                .qrCode(token.getQrCode())
                .channels(token.getChannels())
                .paymentChannel(token.getPaymentChannel())
                .paymentOperator(token.getPaymentOperator())
                .expiresAt(token.getExpiresAt())
                .rawResponse(token.getCreatedAt() != null ? token.getCreatedAt().toString() : null)
                .build();
    }

    private FatouratiToken toDomain(FatouratiTokenEntity entity) {
        return FatouratiToken.builder()
                .id(entity.getId())
                .tokenRef(entity.getTokenRef())
                .invoiceId(entity.getInvoiceId())
                .mouvementId(entity.getMouvementId())
                .invoiceLineIds(entity.getInvoiceLineIds())
                .orderId(entity.getOrderId())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .qrCode(entity.getQrCode())
                .channels(entity.getChannels())
                .paymentChannel(entity.getPaymentChannel())
                .paymentOperator(entity.getPaymentOperator())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
