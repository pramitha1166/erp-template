package com.eudext.erp.iam.internal.sod;

/** IAM-7: the resulting effective permission set for a user+company would hold both sides of an active SoD rule. */
public class SegregationOfDutiesViolationException extends RuntimeException {

    public SegregationOfDutiesViolationException(String permissionCodeA, String permissionCodeB) {
        super("Segregation of Duties: cannot hold both '" + permissionCodeA + "' and '" + permissionCodeB + "'");
    }
}
