package com.eudext.erp.admin.internal.entitlement;

/** ADM-5: a Brand admin tried to grant a Tenant an entitlement the Brand itself doesn't have. */
public class EntitlementBoundExceededException extends RuntimeException {

    public EntitlementBoundExceededException(String featureCode) {
        super("Cannot grant '" + featureCode + "' to a tenant: the brand is not entitled to it");
    }
}
