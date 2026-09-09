package com.fret.payment.adapter.out.persistance.adapter;

import com.fret.payment.adapter.out.persistance.entity.FatouratiTransactionEntity;
import com.fret.payment.adapter.out.persistance.repository.FatouratiTransactionJpaRepository;
import com.fret.payment.domain.model.payment.FatouratiTransaction;
import com.fret.payment.domain.port.out.payment.FatouratiTransactionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FatouratiTransactionRepositoryAdapter implements FatouratiTransactionRepositoryPort {

    private final FatouratiTransactionJpaRepository jpaRepository;

    @Override
    public FatouratiTransaction save(FatouratiTransaction transaction) {
        FatouratiTransactionEntity entity = FatouratiTransactionEntity.fromDomain(transaction);
        entity = jpaRepository.save(entity);
        return entity.toDomain();
    }

    @Override
    public List<FatouratiTransaction> findByTokenRef(String tokenRef) {
        return jpaRepository.findByTokenRefOrderByCreatedAtAsc(tokenRef).stream()
                .map(FatouratiTransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<FatouratiTransaction> findByFatouratiTransactionNumber(String fatouratiTransactionNumber) {
        return jpaRepository.findByFatouratiTransactionNumber(fatouratiTransactionNumber).stream()
                .map(FatouratiTransactionEntity::toDomain)
                .toList();
    }
}
