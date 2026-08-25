package com.eudext.erp.iam.internal.session;

/**
 * IAM-1: a refresh token that has already been rotated away (or revoked)
 * was presented again — a strong signal of token theft. All of the user's
 * sessions are revoked as a precaution before this is thrown.
 */
public class RefreshTokenReuseDetectedException extends RuntimeException {

    public RefreshTokenReuseDetectedException() {
        super("Refresh token reuse detected; all sessions revoked");
    }
}
