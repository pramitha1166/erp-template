package com.eudext.erp.iam.internal.totp;

public class InvalidTotpCodeException extends RuntimeException {

    public InvalidTotpCodeException() {
        super("Invalid TOTP code");
    }
}
