package com.eudext.erp.workflow.internal.resolution;

import com.eudext.erp.iam.ApproverDirectoryApi;
import com.eudext.erp.workflow.internal.chain.ApprovalStep;
import com.eudext.erp.workflow.internal.chain.ApproverType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * WF-3: turns a step's (or escalation target's) configured approver — by
 * role, named user, or reporting hierarchy — into the concrete set of user
 * ids eligible to act. {@code referenceUserId} is whoever the resolution is
 * relative to: the document's submitter for a step's own approver, or the
 * currently-assigned approver being escalated from for an escalation
 * target — {@link com.eudext.erp.iam.ApproverDirectoryApi#managerOf} is
 * always walked from that reference, not from any fixed anchor.
 */
@Service
public class ApproverResolutionService {

    private final ApproverDirectoryApi approverDirectoryApi;

    public ApproverResolutionService(ApproverDirectoryApi approverDirectoryApi) {
        this.approverDirectoryApi = approverDirectoryApi;
    }

    public List<UUID> resolve(
            ApproverType type, UUID companyId, UUID roleId, UUID userId, Integer hierarchyLevel, UUID referenceUserId) {
        return switch (type) {
            case ROLE -> approverDirectoryApi.usersWithRole(companyId, roleId);
            case USER -> List.of(userId);
            case HIERARCHY -> walkHierarchy(referenceUserId, hierarchyLevel == null ? 1 : hierarchyLevel)
                    .map(List::of)
                    .orElseGet(List::of);
        };
    }

    /**
     * WF-5: resolves who a task times out to. Uses the step's configured
     * escalation target if one was set (same three-way resolution as the
     * step's own approver); otherwise falls back to the direct manager of
     * whoever the task was assigned to.
     */
    public List<UUID> resolveEscalationTarget(ApprovalStep step, UUID companyId, UUID escalateFromUserId) {
        if (step.getEscalationType() != null) {
            return resolve(
                    step.getEscalationType(),
                    companyId,
                    step.getEscalationRoleId(),
                    step.getEscalationUserId(),
                    step.getEscalationHierarchyLevel(),
                    escalateFromUserId);
        }
        return approverDirectoryApi.managerOf(escalateFromUserId).map(List::of).orElseGet(List::of);
    }

    private Optional<UUID> walkHierarchy(UUID fromUserId, int levels) {
        UUID current = fromUserId;
        for (int i = 0; i < levels; i++) {
            Optional<UUID> manager = approverDirectoryApi.managerOf(current);
            if (manager.isEmpty()) {
                return Optional.empty();
            }
            current = manager.get();
        }
        return Optional.of(current);
    }
}
