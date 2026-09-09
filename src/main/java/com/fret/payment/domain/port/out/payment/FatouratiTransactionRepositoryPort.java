package com.fret.payment.domain.port.out.payment;

import com.fret.payment.domain.model.payment.FatouratiTransaction;

import java.util.List;

public interface FatouratiTransactionRepositoryPort {

    FatouratiTransaction save(FatouratiTransaction transaction);

    List<FatouratiTransaction> findByTokenRef(String tokenRef);

    List<FatouratiTransaction> findByFatouratiTransactionNumber(String fatouratiTransactionNumber);
}
