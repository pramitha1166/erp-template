package com.eudext.erp.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import com.eudext.erp.workflow.internal.chain.ApprovalChain;
import com.eudext.erp.workflow.internal.chain.ApprovalChainService;
import com.eudext.erp.workflow.internal.chain.ConditionOperator;
import com.eudext.erp.workflow.internal.delegation.DelegationService;
import com.eudext.erp.workflow.internal.engine.WorkflowEngine;
import com.eudext.erp.workflow.internal.instance.ApprovalHistoryEntry;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Epic 0.4 (PLAT-WF) end to end against a real Postgres: chain
 * configuration (WF-1), condition gating (WF-2), role/user/hierarchy
 * approver resolution (WF-3), sequential and parallel step execution
 * (WF-4), delegation (WF-5), mandatory-comment rejection (WF-6), and the
 * resulting approval history (WF-7).
 */
class WorkflowEngineIT extends AbstractIntegrationTest {

    @Autowired
    private WorkflowApi workflowApi;

    @Autowired
    private WorkflowEngine engine;

    @Autowired
    private ApprovalChainService chainService;

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private IdentityProvisioningApi identityProvisioningApi;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private static final String DOCUMENT_TYPE = "TEST_EXPENSE_CLAIM";

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(tenantId);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void noActiveChainMeansApprovalIsNotRequired() {
        UUID documentId = UUID.randomUUID();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();

        WorkflowApi.Outcome outcome = workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, "NO_CHAIN_DOC_TYPE", documentId, submitter, Map.of()));

        assertThat(outcome).isEqualTo(WorkflowApi.Outcome.NOT_REQUIRED);
    }

    @Test
    void singleUserStepApprovalGrantsTheInstanceAndRecordsHistory() {
        UUID approverId = identityProvisioningApi.provisionTenantUser(tenantId, unique("approver")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Single approver");
        chainService.addUserStep(tenantId, chain.getId(), 1, "Manager approval", approverId);
        UUID documentId = UUID.randomUUID();

        WorkflowApi.Outcome outcome = workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of()));
        assertThat(outcome).isEqualTo(WorkflowApi.Outcome.PENDING);
        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.PENDING);

        var pending = engine.pendingTasksFor(approverId);
        assertThat(pending).hasSize(1);
        UUID taskId = pending.get(0).task().getId();

        engine.decide(taskId, approverId, true, "looks fine");

        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.APPROVED);
        List<ApprovalHistoryEntry> history = engine.historyOf(DOCUMENT_TYPE, documentId);
        assertThat(history).extracting(ApprovalHistoryEntry::getAction).containsExactly("STARTED", "APPROVED", "INSTANCE_APPROVED");
    }

    @Test
    void rejectionWithoutACommentIsRejectedByTheEngine() {
        UUID approverId = identityProvisioningApi.provisionTenantUser(tenantId, unique("approver")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Single approver");
        chainService.addUserStep(tenantId, chain.getId(), 1, "Manager approval", approverId);
        UUID documentId = UUID.randomUUID();
        workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of()));
        UUID taskId = engine.pendingTasksFor(approverId).get(0).task().getId();

        assertThatThrownBy(() -> engine.decide(taskId, approverId, false, ""))
                .isInstanceOf(MandatoryCommentMissingException.class);
        assertThatThrownBy(() -> engine.decide(taskId, approverId, false, null))
                .isInstanceOf(MandatoryCommentMissingException.class);
        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.PENDING);

        engine.decide(taskId, approverId, false, "Missing receipts");

        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.REJECTED);
    }

    @Test
    void sequentialStepsAdvanceOneGroupAtATime() {
        UUID firstApprover = identityProvisioningApi.provisionTenantUser(tenantId, unique("first")).userId();
        UUID secondApprover = identityProvisioningApi.provisionTenantUser(tenantId, unique("second")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Two-level");
        chainService.addUserStep(tenantId, chain.getId(), 1, "Manager", firstApprover);
        chainService.addUserStep(tenantId, chain.getId(), 2, "Director", secondApprover);
        UUID documentId = UUID.randomUUID();

        workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of()));

        assertThat(engine.pendingTasksFor(firstApprover)).hasSize(1);
        assertThat(engine.pendingTasksFor(secondApprover)).isEmpty();

        engine.decide(engine.pendingTasksFor(firstApprover).get(0).task().getId(), firstApprover, true, null);

        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.PENDING);
        assertThat(engine.pendingTasksFor(secondApprover)).hasSize(1);

        engine.decide(engine.pendingTasksFor(secondApprover).get(0).task().getId(), secondApprover, true, null);

        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.APPROVED);
    }

    @Test
    void parallelStepsInTheSameGroupBothMustBeSatisfied() {
        UUID financeApprover = identityProvisioningApi.provisionTenantUser(tenantId, unique("finance")).userId();
        UUID opsApprover = identityProvisioningApi.provisionTenantUser(tenantId, unique("ops")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Parallel");
        chainService.addUserStep(tenantId, chain.getId(), 1, "Finance", financeApprover);
        chainService.addUserStep(tenantId, chain.getId(), 1, "Operations", opsApprover);
        UUID documentId = UUID.randomUUID();

        workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of()));
        assertThat(engine.pendingTasksFor(financeApprover)).hasSize(1);
        assertThat(engine.pendingTasksFor(opsApprover)).hasSize(1);

        engine.decide(engine.pendingTasksFor(financeApprover).get(0).task().getId(), financeApprover, true, null);
        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.PENDING);

        engine.decide(engine.pendingTasksFor(opsApprover).get(0).task().getId(), opsApprover, true, null);
        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.APPROVED);
    }

    @Test
    void roleStepIsSatisfiedByAnyOneHolderAndCancelsTheOthers() {
        UUID holder1 = identityProvisioningApi.provisionTenantUser(tenantId, unique("holder1")).userId();
        UUID holder2 = identityProvisioningApi.provisionTenantUser(tenantId, unique("holder2")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        UUID roleId = identityProvisioningApi.createRole(tenantId, "Approvers-" + UUID.randomUUID(), null);
        identityProvisioningApi.assignRole(tenantId, holder1, companyId, roleId, "test");
        identityProvisioningApi.assignRole(tenantId, holder2, companyId, roleId, "test");
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Role step");
        chainService.addRoleStep(tenantId, chain.getId(), 1, "Any approver", roleId);
        UUID documentId = UUID.randomUUID();

        workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of()));
        assertThat(engine.pendingTasksFor(holder1)).hasSize(1);
        assertThat(engine.pendingTasksFor(holder2)).hasSize(1);

        engine.decide(engine.pendingTasksFor(holder1).get(0).task().getId(), holder1, true, null);

        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.APPROVED);
        assertThat(engine.pendingTasksFor(holder2)).isEmpty();
    }

    @Test
    void conditionGatesWhetherTheStepApplies() {
        UUID approverId = identityProvisioningApi.provisionTenantUser(tenantId, unique("approver")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Threshold-gated");
        var step = chainService.addUserStep(tenantId, chain.getId(), 1, "Director for large amounts", approverId);
        chainService.addNumberCondition(tenantId, step.getId(), "amount", ConditionOperator.GT, new BigDecimal("500000"));

        UUID belowThresholdDoc = UUID.randomUUID();
        WorkflowApi.Outcome belowOutcome = workflowApi.startApproval(new WorkflowApi.StartApprovalRequest(
                tenantId, companyId, branchId, DOCUMENT_TYPE, belowThresholdDoc, submitter, Map.of("amount", new BigDecimal("100000"))));
        assertThat(belowOutcome).isEqualTo(WorkflowApi.Outcome.NOT_REQUIRED);

        UUID aboveThresholdDoc = UUID.randomUUID();
        WorkflowApi.Outcome aboveOutcome = workflowApi.startApproval(new WorkflowApi.StartApprovalRequest(
                tenantId, companyId, branchId, DOCUMENT_TYPE, aboveThresholdDoc, submitter, Map.of("amount", new BigDecimal("600000"))));
        assertThat(aboveOutcome).isEqualTo(WorkflowApi.Outcome.PENDING);
    }

    @Test
    void hierarchyStepResolvesTheSubmittersManager() {
        UUID managerId = identityProvisioningApi.provisionTenantUser(tenantId, unique("manager")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        identityProvisioningApi.setManager(tenantId, submitter, managerId);
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Manager sign-off");
        chainService.addHierarchyStep(tenantId, chain.getId(), 1, "Direct manager", 1);
        UUID documentId = UUID.randomUUID();

        workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of()));

        assertThat(engine.pendingTasksFor(managerId)).hasSize(1);
    }

    @Test
    void delegateMayDecideOnTheDelegatorsBehalfWithinTheDateRange() {
        UUID approverId = identityProvisioningApi.provisionTenantUser(tenantId, unique("approver")).userId();
        UUID delegateId = identityProvisioningApi.provisionTenantUser(tenantId, unique("delegate")).userId();
        UUID strangerId = identityProvisioningApi.provisionTenantUser(tenantId, unique("stranger")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        delegationService.delegate(tenantId, approverId, delegateId, LocalDate.now().minusDays(1), LocalDate.now().plusDays(7), "on leave");
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Delegatable");
        chainService.addUserStep(tenantId, chain.getId(), 1, "Manager", approverId);
        UUID documentId = UUID.randomUUID();
        workflowApi.startApproval(
                new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of()));
        UUID taskId = engine.pendingTasksFor(approverId).get(0).task().getId();

        assertThatThrownBy(() -> engine.decide(taskId, strangerId, true, null)).isInstanceOf(NotAssignedApproverException.class);

        engine.decide(taskId, delegateId, true, null);

        assertThat(workflowApi.statusOf(DOCUMENT_TYPE, documentId)).isEqualTo(WorkflowApi.Status.APPROVED);
    }

    @Test
    void startingApprovalTwiceForTheSameDocumentReturnsAlreadyPending() {
        UUID approverId = identityProvisioningApi.provisionTenantUser(tenantId, unique("approver")).userId();
        UUID submitter = identityProvisioningApi.provisionTenantUser(tenantId, unique("submitter")).userId();
        ApprovalChain chain = chainService.createChain(tenantId, companyId, DOCUMENT_TYPE, "Single approver");
        chainService.addUserStep(tenantId, chain.getId(), 1, "Manager", approverId);
        UUID documentId = UUID.randomUUID();

        assertThat(workflowApi.startApproval(
                        new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of())))
                .isEqualTo(WorkflowApi.Outcome.PENDING);
        assertThat(workflowApi.startApproval(
                        new WorkflowApi.StartApprovalRequest(tenantId, companyId, branchId, DOCUMENT_TYPE, documentId, submitter, Map.of())))
                .isEqualTo(WorkflowApi.Outcome.ALREADY_PENDING);
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.test";
    }
}
