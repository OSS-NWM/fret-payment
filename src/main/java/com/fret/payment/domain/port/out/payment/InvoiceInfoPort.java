package com.fret.payment.domain.port.out.payment;

import com.fret.payment.domain.model.payment.InvoiceInfo;

import java.util.List;

public interface InvoiceInfoPort {
    InvoiceInfo findById(Long invoiceId);
    List<InvoiceInfo> findByIds(List<Long> invoiceIds);
    List<InvoiceInfo> findByMouvementId(String mouvementId);
}
