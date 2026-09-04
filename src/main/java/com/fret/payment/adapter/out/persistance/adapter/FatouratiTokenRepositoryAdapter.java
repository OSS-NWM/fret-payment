package com.fret.payment.adapter.out.persistance.adapter;

import com.fret.payment.adapter.out.persistance.entity.FatouratiTokenEntity;
import com.fret.payment.adapter.out.persistance.repository.FatouratiTokenJpaRepository;
import com.fret.payment.domain.model.payment.FatouratiToken;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.port.out.payment.FatouratiTokenRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FatouratiTokenRepositoryAdapter implements FatouratiTokenRepositoryPort {

    private final FatouratiTokenJpaRepository jpaRepository;

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
    public void updateStatus(String tokenRef, FatouratiTokenStatus status) {
        jpaRepository.findByTokenRef(tokenRef).ifPresent(entity -> {
            entity.setStatus(status);
            jpaRepository.save(entity);
        });
    }

    @Override
    public Optional<FatouratiToken> findActiveByMouvementId(String mouvementId) {
        return jpaRepository.findByMouvementIdAndStatus(mouvementId, FatouratiTokenStatus.CREATED)
                .map(this::toDomain);
    }

    private FatouratiTokenEntity toEntity(FatouratiToken token) {
        return FatouratiTokenEntity.builder()
                .id(token.getId())
                .tokenRef(token.getTokenRef())
                .mouvementId(token.getMouvementId())
                .orderId(token.getOrderId())
                .totalAmount(token.getTotalAmount())
                .currency(token.getCurrency())
                .status(token.getStatus())
                .qrCode(token.getQrCode())
                .channels(token.getChannels())
                .expiresAt(token.getExpiresAt())
                .rawResponse(token.getCreatedAt() != null ? token.getCreatedAt().toString() : null)
                .build();
    }

    private FatouratiToken toDomain(FatouratiTokenEntity entity) {
        return FatouratiToken.builder()
                .id(entity.getId())
                .tokenRef(entity.getTokenRef())
                .mouvementId(entity.getMouvementId())
                .orderId(entity.getOrderId())
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .status(entity.getStatus())
                .qrCode(entity.getQrCode())
                .channels(entity.getChannels())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
