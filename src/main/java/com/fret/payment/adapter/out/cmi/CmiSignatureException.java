package com.fret.payment.adapter.out.cmi;

public class CmiSignatureException extends RuntimeException {
    private final String signatureData;
    private final String signatureValue;

    public CmiSignatureException(String message, String signatureData, String signatureValue, Throwable cause) {
        super(message, cause);
        this.signatureData = signatureData;
        this.signatureValue = signatureValue;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public String getSignatureValue() {
        return signatureValue;
    }
}
