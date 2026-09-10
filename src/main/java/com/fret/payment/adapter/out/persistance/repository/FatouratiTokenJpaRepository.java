package com.fret.payment.adapter.out.persistance.repository;

import com.fret.payment.adapter.out.persistance.entity.FatouratiTokenEntity;
import com.fret.payment.domain.model.payment.FatouratiTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FatouratiTokenJpaRepository extends JpaRepository<FatouratiTokenEntity, Long> {

    Optional<FatouratiTokenEntity> findByTokenRef(String tokenRef);

    Optional<FatouratiTokenEntity> findByMouvementId(String mouvementId);

    Optional<FatouratiTokenEntity> findByMouvementIdAndStatus(String mouvementId, FatouratiTokenStatus status);

    Optional<FatouratiTokenEntity> findByInvoiceIdAndStatus(Long invoiceId, FatouratiTokenStatus status);

    Optional<FatouratiTokenEntity> findByOrderId(String orderId);
}
