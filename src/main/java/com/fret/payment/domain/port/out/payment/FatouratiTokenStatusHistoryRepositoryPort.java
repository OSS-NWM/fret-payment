package com.fret.payment.domain.port.out.payment;

import com.fret.payment.domain.model.payment.FatouratiTokenStatusHistory;

import java.util.List;

public interface FatouratiTokenStatusHistoryRepositoryPort {

    void save(FatouratiTokenStatusHistory history);

    List<FatouratiTokenStatusHistory> findByTokenRef(String tokenRef);
}
