package com.eudext.erp.iam.internal.password;

import java.util.List;

/** IAM-9: a candidate password fails one or more of the tenant's configured rules. */
public class PasswordPolicyViolationException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyViolationException(List<String> violations) {
        super("Password policy violated: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
