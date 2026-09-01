package com.eudext.erp.workflow.internal.delegation;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** WF-5: date-ranged delegation of a user's own approval authority. */
@Service
public class DelegationService {

    private final ApprovalDelegationRepository repository;

    public DelegationService(ApprovalDelegationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ApprovalDelegation delegate(
            UUID tenantId, UUID delegatorUserId, UUID delegateUserId, LocalDate startDate, LocalDate endDate, String reason) {
        return repository.save(ApprovalDelegation.create(tenantId, delegatorUserId, delegateUserId, startDate, endDate, reason));
    }

    @Transactional
    public void revoke(UUID delegationId, UUID requestedBy) {
        ApprovalDelegation delegation =
                repository.findById(delegationId).orElseThrow(() -> new NoSuchElementException("No such delegation"));
        if (!delegation.getDelegatorUserId().equals(requestedBy)) {
            throw new IllegalArgumentException("Only the delegator may revoke their own delegation");
        }
        delegation.revoke();
    }

    @Transactional(readOnly = true)
    public List<ApprovalDelegation> delegationsOf(UUID delegatorUserId) {
        return repository.findByDelegatorUserId(delegatorUserId);
    }

    /** WF-5: true if {@code actingUserId} is either {@code assignedUserId} itself, or an active delegate of theirs on {@code date}. */
    @Transactional(readOnly = true)
    public boolean isAuthorizedActor(UUID assignedUserId, UUID actingUserId, LocalDate date) {
        if (assignedUserId.equals(actingUserId)) {
            return true;
        }
        return repository.findByDelegatorUserIdAndRevokedFalse(assignedUserId).stream()
                .anyMatch(delegation -> delegation.getDelegateUserId().equals(actingUserId) && delegation.isActiveOn(date));
    }
}
