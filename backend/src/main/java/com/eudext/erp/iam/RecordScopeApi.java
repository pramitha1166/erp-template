package com.eudext.erp.iam;

import java.util.Set;
import java.util.UUID;

/**
 * IAM-6: public entry point other modules use to find out whether a user's
 * effective roles restrict visibility on a warehouse/cost-centre/branch
 * dimension, without reaching into IAM's internal tables directly
 * (ARCH-1). Coordinate with Epic 0.9, which gives {@code scopeValue} a real
 * master (Branch etc.) — until then it is an opaque id, same convention as
 * {@code Document.branchId}.
 */
public interface RecordScopeApi {

    /**
     * True if the user may see/act on a record whose {@code scopeType}
     * dimension (e.g. branch) is {@code scopeValue}, across every role the
     * user holds in {@code companyId}. A role with no restrictions on that
     * dimension permits every value; a role with one or more restrictions
     * permits only the listed ones. The user is permitted overall if *any*
     * held role permits it. A user holding no role in the company is never
     * permitted.
     */
    boolean isRecordVisible(UUID userId, UUID companyId, RecordScopeType scopeType, UUID scopeValue);

    /**
     * True if at least one role the user holds in {@code companyId} has no
     * restriction on {@code scopeType} — i.e. the user can see every value
     * on that dimension, and {@link #allowedScopeValues} would be
     * misleadingly incomplete. Callers building a {@code WHERE branch_id
     * IN (...)} clause should check this first and skip the filter
     * entirely when true.
     */
    boolean isUnrestricted(UUID userId, UUID companyId, RecordScopeType scopeType);

    /**
     * The union of scope values every restricted role the user holds
     * permits on {@code scopeType}. Only meaningful when {@link
     * #isUnrestricted} is false for the same arguments.
     */
    Set<UUID> allowedScopeValues(UUID userId, UUID companyId, RecordScopeType scopeType);
}
