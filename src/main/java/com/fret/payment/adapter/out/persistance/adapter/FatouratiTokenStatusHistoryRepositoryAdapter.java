package com.fret.payment.adapter.out.persistance.adapter;

import com.fret.payment.adapter.out.persistance.entity.FatouratiTokenStatusHistoryEntity;
import com.fret.payment.adapter.out.persistance.repository.FatouratiTokenStatusHistoryJpaRepository;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import com.fret.payment.domain.model.payment.FatouratiTokenStatusHistory;
import com.fret.payment.domain.port.out.payment.FatouratiTokenStatusHistoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FatouratiTokenStatusHistoryRepositoryAdapter implements FatouratiTokenStatusHistoryRepositoryPort {

    private final FatouratiTokenStatusHistoryJpaRepository jpaRepository;

    @Override
    public void save(FatouratiTokenStatusHistory history) {
        FatouratiTokenStatusHistoryEntity entity = FatouratiTokenStatusHistoryEntity.builder()
                .tokenRef(history.getTokenRef())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .actor(history.getActor())
                .channel(history.getChannel())
                .operator(history.getOperator())
                .build();
        jpaRepository.save(entity);
    }

    @Override
    public List<FatouratiTokenStatusHistory> findByTokenRef(String tokenRef) {
        return jpaRepository.findByTokenRefOrderByOccurredAtAsc(tokenRef).stream()
                .map(this::toDomain)
                .toList();
    }

    private FatouratiTokenStatusHistory toDomain(FatouratiTokenStatusHistoryEntity entity) {
        return FatouratiTokenStatusHistory.builder()
                .id(entity.getId())
                .tokenRef(entity.getTokenRef())
                .previousStatus(entity.getPreviousStatus())
                .newStatus(entity.getNewStatus())
                .reason(entity.getReason())
                .actor(entity.getActor())
                .channel(entity.getChannel())
                .operator(entity.getOperator())
                .occurredAt(entity.getOccurredAt())
                .build();
    }
}
