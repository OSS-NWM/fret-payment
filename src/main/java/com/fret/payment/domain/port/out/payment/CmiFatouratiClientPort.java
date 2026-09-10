package com.fret.payment.domain.port.out.payment;

import com.fret.payment.adapter.out.cmi.InvoiceLinePayload;
import com.fret.payment.domain.model.payment.*;

import java.math.BigDecimal;
import java.util.List;

public interface CmiFatouratiClientPort {

    String getAccessToken();

    FatouratiToken generateToken(Long invoiceId, String orderId, List<InvoiceLinePayload> items,
                                  BigDecimal totalAmount, String currency,
                                  String callbackUrl, String cancelUrl, String checkStatusUrl);

    FatouratiToken getTokenByRef(String tokenRef);

    FatouratiToken getTokenByOrderId(String orderId);

    FatouratiTransactionStatus getTransactionStatus(String fatouratiTransactionNumber);

    void updateTransactionStatus(String fatouratiTransactionNumber, FatouratiTransactionStatus status);

    void cancelToken(String tokenRef);

    List<String> getChannels(String tokenRef);
}
