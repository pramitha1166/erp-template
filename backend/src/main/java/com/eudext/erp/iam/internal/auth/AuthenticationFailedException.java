package com.eudext.erp.iam.internal.auth;

/** Deliberately generic message — never distinguish "no such user" from "wrong password" to a caller (avoids user enumeration). */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
