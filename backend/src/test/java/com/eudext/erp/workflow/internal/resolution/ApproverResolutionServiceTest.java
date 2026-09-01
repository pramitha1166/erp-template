package com.eudext.erp.workflow.internal.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.eudext.erp.iam.ApproverDirectoryApi;
import com.eudext.erp.workflow.internal.chain.ApprovalStep;
import com.eudext.erp.workflow.internal.chain.ApproverType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** WF-3: approver resolution by role, named user, and reporting hierarchy. */
@ExtendWith(MockitoExtension.class)
class ApproverResolutionServiceTest {

    @Mock
    private ApproverDirectoryApi approverDirectoryApi;

    private ApproverResolutionService service;

    private final UUID companyId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID submitter = UUID.randomUUID();

    @Test
    void roleResolvesToEveryHolderInCompany() {
        service = new ApproverResolutionService(approverDirectoryApi);
        UUID holder1 = UUID.randomUUID();
        UUID holder2 = UUID.randomUUID();
        when(approverDirectoryApi.usersWithRole(companyId, roleId)).thenReturn(List.of(holder1, holder2));

        List<UUID> resolved = service.resolve(ApproverType.ROLE, companyId, roleId, null, null, submitter);

        assertThat(resolved).containsExactlyInAnyOrder(holder1, holder2);
    }

    @Test
    void userResolvesToItsConfiguredUserId() {
        service = new ApproverResolutionService(approverDirectoryApi);

        List<UUID> resolved = service.resolve(ApproverType.USER, companyId, null, userId, null, submitter);

        assertThat(resolved).containsExactly(userId);
    }

    @Test
    void hierarchyLevelOneResolvesDirectManager() {
        service = new ApproverResolutionService(approverDirectoryApi);
        UUID manager = UUID.randomUUID();
        when(approverDirectoryApi.managerOf(submitter)).thenReturn(Optional.of(manager));

        List<UUID> resolved = service.resolve(ApproverType.HIERARCHY, companyId, null, null, 1, submitter);

        assertThat(resolved).containsExactly(manager);
    }

    @Test
    void hierarchyLevelTwoWalksTwoLevelsUp() {
        service = new ApproverResolutionService(approverDirectoryApi);
        UUID manager = UUID.randomUUID();
        UUID skipLevelManager = UUID.randomUUID();
        when(approverDirectoryApi.managerOf(submitter)).thenReturn(Optional.of(manager));
        when(approverDirectoryApi.managerOf(manager)).thenReturn(Optional.of(skipLevelManager));

        List<UUID> resolved = service.resolve(ApproverType.HIERARCHY, companyId, null, null, 2, submitter);

        assertThat(resolved).containsExactly(skipLevelManager);
    }

    @Test
    void hierarchyResolvesToEmptyWhenChainBreaksBeforeRequestedLevel() {
        service = new ApproverResolutionService(approverDirectoryApi);
        when(approverDirectoryApi.managerOf(submitter)).thenReturn(Optional.empty());

        List<UUID> resolved = service.resolve(ApproverType.HIERARCHY, companyId, null, null, 1, submitter);

        assertThat(resolved).isEmpty();
    }

    @Test
    void escalationTargetFallsBackToAssigneesManagerWhenStepHasNoEscalationConfigured() {
        service = new ApproverResolutionService(approverDirectoryApi);
        ApprovalStep step = ApprovalStep.user(UUID.randomUUID(), UUID.randomUUID(), 1, "Manager approval", userId);
        UUID manager = UUID.randomUUID();
        when(approverDirectoryApi.managerOf(userId)).thenReturn(Optional.of(manager));

        List<UUID> resolved = service.resolveEscalationTarget(step, companyId, userId);

        assertThat(resolved).containsExactly(manager);
    }
}
