package com.eudext.erp.iam.internal.rbac;

/** IAM-2: the role being assigned carries an `:approve` permission, so the target user must have TOTP 2FA enabled first. */
public class TotpRequiredException extends RuntimeException {

    public TotpRequiredException() {
        super("This role grants an approval permission; the user must enable TOTP 2FA before it can be assigned");
    }
}
