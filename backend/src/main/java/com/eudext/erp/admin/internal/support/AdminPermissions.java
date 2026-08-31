package com.eudext.erp.admin.internal.support;

/** ADM-1..ADM-9 permission codes. See {@code Brand}/{@code Tenant} javadoc's design note on why these are distinct scopes, not "IAM-3 roles with a higher count". */
public final class AdminPermissions {

    public static final String PLATFORM_MANAGE = "admin:platform:manage";
    public static final String BRAND_MANAGE = "admin:brand:manage";
    public static final String TENANT_ONBOARD = "admin:tenant:onboard";
    public static final String TENANT_MANAGE = "admin:tenant:manage";
    public static final String TENANT_INVITE = "admin:tenant:invite";
    public static final String IMPERSONATION_START = "admin:impersonation:start";
    public static final String DATA_REQUEST_MANAGE = "admin:datarequest:manage";
    public static final String USAGE_READ = "admin:usage:read";

    private AdminPermissions() {}
}
