package com.eudext.erp.iam;

/**
 * Deliberately generic message — never distinguish "no such user" from
 * "wrong password" to a caller (avoids user enumeration). Public so
 * {@link AuthenticationApi} callers outside this module (the admin realm's
 * own login entry point) can catch it without reaching into {@code
 * iam.internal}.
 */
public class AuthenticationFailedException extends RuntimeException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}
