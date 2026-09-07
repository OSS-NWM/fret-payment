package com.fret.payment.adapter.out.persistance.repository;

import com.fret.payment.adapter.out.persistance.entity.FatouratiTokenStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FatouratiTokenStatusHistoryJpaRepository extends JpaRepository<FatouratiTokenStatusHistoryEntity, Long> {

    List<FatouratiTokenStatusHistoryEntity> findByTokenRefOrderByOccurredAtAsc(String tokenRef);
}
