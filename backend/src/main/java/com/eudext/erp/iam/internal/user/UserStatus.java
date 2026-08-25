package com.eudext.erp.iam.internal.user;

/** MDM-10-style soft state: users are disabled, never hard-deleted. */
public enum UserStatus {
    ACTIVE,
    DISABLED
}
