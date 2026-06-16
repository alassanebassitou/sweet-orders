package com.devcrafter.Patisserie.App.Exceptions;

import lombok.Getter;

public class KkiapayPaymentException extends RuntimeException {

    @Getter
    private final String kkiapayStatus;
    @Getter
    private final String frontendStatus;
    public KkiapayPaymentException(String kkiapayStatus, String frontendStatus) {
        super("Transaction invalid: " + kkiapayStatus);
        this.kkiapayStatus = kkiapayStatus;
        this.frontendStatus = frontendStatus;
    }
}
