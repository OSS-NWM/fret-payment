package com.fret.payment.adapter.out.persistance.repository;

import com.fret.payment.adapter.out.persistance.entity.FatouratiTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FatouratiTransactionJpaRepository extends JpaRepository<FatouratiTransactionEntity, Long> {

    List<FatouratiTransactionEntity> findByTokenRefOrderByCreatedAtAsc(String tokenRef);

    List<FatouratiTransactionEntity> findByFatouratiTransactionNumber(String fatouratiTransactionNumber);
}
