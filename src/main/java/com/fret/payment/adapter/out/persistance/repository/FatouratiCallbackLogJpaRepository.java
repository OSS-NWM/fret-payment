package com.fret.payment.adapter.out.persistance.repository;

import com.fret.payment.adapter.out.persistance.entity.FatouratiCallbackLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FatouratiCallbackLogJpaRepository extends JpaRepository<FatouratiCallbackLogEntity, Long> {

    boolean existsByNumTrxFatourati(String numTrxFatourati);

    Optional<FatouratiCallbackLogEntity> findByTokenRef(String tokenRef);
}
