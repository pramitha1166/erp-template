package com.eudext.erp.workflow.internal.engine;

import com.eudext.erp.workflow.IllegalWorkflowStateException;
import com.eudext.erp.workflow.MandatoryCommentMissingException;
import com.eudext.erp.workflow.NoApproverResolvedException;
import com.eudext.erp.workflow.NotAssignedApproverException;
import com.eudext.erp.workflow.WorkflowApi;
import com.eudext.erp.workflow.WorkflowDecisionEvents;
import com.eudext.erp.workflow.internal.chain.ApprovalChain;
import com.eudext.erp.workflow.internal.chain.ApprovalStep;
import com.eudext.erp.workflow.internal.chain.ApprovalStepConditionRepository;
import com.eudext.erp.workflow.internal.chain.ApprovalStepRepository;
import com.eudext.erp.workflow.internal.condition.ConditionEvaluator;
import com.eudext.erp.workflow.internal.delegation.DelegationService;
import com.eudext.erp.workflow.internal.instance.ApprovalHistoryEntry;
import com.eudext.erp.workflow.internal.instance.ApprovalHistoryRepository;
import com.eudext.erp.workflow.internal.instance.ApprovalTask;
import com.eudext.erp.workflow.internal.instance.ApprovalTaskRepository;
import com.eudext.erp.workflow.internal.instance.InstanceStatus;
import com.eudext.erp.workflow.internal.instance.TaskStatus;
import com.eudext.erp.workflow.internal.instance.WorkflowInstance;
import com.eudext.erp.workflow.internal.instance.WorkflowInstanceRepository;
import com.eudext.erp.workflow.internal.instance.WorkflowInstanceStep;
import com.eudext.erp.workflow.internal.instance.WorkflowInstanceStepRepository;
import com.eudext.erp.workflow.internal.resolution.ApproverResolutionService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WF-1..WF-6: the approval-chain orchestrator. See {@link WorkflowApi}'s
 * javadoc for the integration contract document-owning modules follow —
 * this class implements the mechanics that contract relies on: resolving
 * which chain and steps apply, creating tasks group by group (WF-4),
 * advancing on approval, and rejecting the whole instance with a mandatory
 * comment (WF-6) the moment any one task is rejected.
 */
@Service
public class WorkflowEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowEngine.class);

    private final ApprovalStepRepository stepRepository;
    private final ApprovalStepConditionRepository conditionRepository;
    private final ConditionEvaluator conditionEvaluator;
    private final ApproverResolutionService approverResolutionService;
    private final DelegationService delegationService;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowInstanceStepRepository planStepRepository;
    private final ApprovalTaskRepository taskRepository;
    private final ApprovalHistoryRepository historyRepository;
    private final com.eudext.erp.workflow.internal.chain.ApprovalChainRepository chainRepository;
    private final ApplicationEventPublisher events;

    public WorkflowEngine(
            ApprovalStepRepository stepRepository,
            ApprovalStepConditionRepository conditionRepository,
            ConditionEvaluator conditionEvaluator,
            ApproverResolutionService approverResolutionService,
            DelegationService delegationService,
            WorkflowInstanceRepository instanceRepository,
            WorkflowInstanceStepRepository planStepRepository,
            ApprovalTaskRepository taskRepository,
            ApprovalHistoryRepository historyRepository,
            com.eudext.erp.workflow.internal.chain.ApprovalChainRepository chainRepository,
            ApplicationEventPublisher events) {
        this.stepRepository = stepRepository;
        this.conditionRepository = conditionRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.approverResolutionService = approverResolutionService;
        this.delegationService = delegationService;
        this.instanceRepository = instanceRepository;
        this.planStepRepository = planStepRepository;
        this.taskRepository = taskRepository;
        this.historyRepository = historyRepository;
        this.chainRepository = chainRepository;
        this.events = events;
    }

    @Transactional
    public WorkflowApi.Outcome startApproval(WorkflowApi.StartApprovalRequest request) {
        ApprovalChain chain = chainRepository
                .findByCompanyIdAndDocumentTypeAndActiveTrue(request.companyId(), request.documentType())
                .orElse(null);
        if (chain == null) {
            return WorkflowApi.Outcome.NOT_REQUIRED;
        }
        if (instanceRepository
                .findByDocumentTypeAndDocumentIdAndStatus(request.documentType(), request.documentId(), InstanceStatus.PENDING)
                .isPresent()) {
            return WorkflowApi.Outcome.ALREADY_PENDING;
        }

        List<ApprovalStep> steps = stepRepository.findByChainIdOrderBySequenceOrderAsc(chain.getId());
        List<ApprovalStep> applicable = steps.stream()
                .filter(step -> conditionEvaluator.matches(conditionRepository.findByStepId(step.getId()), request.fieldValues()))
                .toList();
        if (applicable.isEmpty()) {
            return WorkflowApi.Outcome.NOT_REQUIRED;
        }

        WorkflowInstance instance = instanceRepository.save(WorkflowInstance.start(
                request.tenantId(),
                request.companyId(),
                request.branchId(),
                request.documentType(),
                request.documentId(),
                chain.getId(),
                request.submittedBy()));

        for (ApprovalStep step : applicable) {
            planStepRepository.save(WorkflowInstanceStep.of(request.tenantId(), instance.getId(), step.getId(), step.getSequenceOrder()));
        }
        historyRepository.save(
                ApprovalHistoryEntry.of(request.tenantId(), instance.getId(), null, "STARTED", request.submittedBy(), null));

        int firstGroup = applicable.stream().mapToInt(ApprovalStep::getSequenceOrder).min().orElseThrow();
        List<ApprovalStep> firstGroupSteps = applicable.stream().filter(s -> s.getSequenceOrder() == firstGroup).toList();
        createTasksForGroup(instance, firstGroupSteps, request.submittedBy());

        return WorkflowApi.Outcome.PENDING;
    }

    @Transactional
    public void decide(UUID taskId, UUID actingUserId, boolean approve, String comment) {
        ApprovalTask task = taskRepository.findById(taskId).orElseThrow(() -> new NoSuchElementException("No such approval task"));
        WorkflowInstance instance =
                instanceRepository.findById(task.getInstanceId()).orElseThrow(() -> new NoSuchElementException("No such workflow instance"));
        if (instance.getStatus() != InstanceStatus.PENDING) {
            throw new IllegalWorkflowStateException("Workflow instance " + instance.getId() + " is no longer pending");
        }
        if (!delegationService.isAuthorizedActor(task.getAssignedUserId(), actingUserId, LocalDate.now())) {
            throw new NotAssignedApproverException("User " + actingUserId + " is not authorized to decide task " + taskId);
        }
        if (!approve && (comment == null || comment.isBlank())) {
            throw new MandatoryCommentMissingException("WF-6: a rejection comment is required");
        }

        if (approve) {
            approveTask(task, instance, actingUserId, comment);
        } else {
            rejectTask(task, instance, actingUserId, comment);
        }
    }

    /** WF-5: sweeps every {@code PENDING} task whose {@code dueAt} has passed and reassigns it to its escalation target. */
    @Transactional
    public int escalateDueTasks(Instant now) {
        List<ApprovalTask> due = taskRepository.findByStatusAndDueAtBefore(TaskStatus.PENDING, now);
        int escalated = 0;
        for (ApprovalTask task : due) {
            if (escalateOne(task)) {
                escalated++;
            }
        }
        return escalated;
    }

    /** WF-8: a user's pending-approval inbox, each task paired with the document it's blocking. */
    @Transactional(readOnly = true)
    public List<TaskWithDocument> pendingTasksFor(UUID userId) {
        return taskRepository.findByAssignedUserIdAndStatus(userId, TaskStatus.PENDING).stream()
                .map(task -> new TaskWithDocument(task, instanceRepository.findById(task.getInstanceId()).orElseThrow()))
                .toList();
    }

    public record TaskWithDocument(ApprovalTask task, WorkflowInstance instance) {}

    @Transactional(readOnly = true)
    public List<ApprovalHistoryEntry> historyOf(String documentType, UUID documentId) {
        return instanceRepository.findByDocumentTypeAndDocumentIdOrderByCreatedAtAsc(documentType, documentId).stream()
                .flatMap(instance -> historyRepository.findByInstanceIdOrderByOccurredAtAsc(instance.getId()).stream())
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowApi.Status statusOf(String documentType, UUID documentId) {
        List<WorkflowInstance> instances = instanceRepository.findByDocumentTypeAndDocumentIdOrderByCreatedAtAsc(documentType, documentId);
        if (instances.isEmpty()) {
            return null;
        }
        return switch (instances.get(instances.size() - 1).getStatus()) {
            case PENDING -> WorkflowApi.Status.PENDING;
            case APPROVED -> WorkflowApi.Status.APPROVED;
            case REJECTED -> WorkflowApi.Status.REJECTED;
            case CANCELLED -> WorkflowApi.Status.CANCELLED;
        };
    }

    @Transactional
    public void cancelPending(String documentType, UUID documentId) {
        instanceRepository
                .findByDocumentTypeAndDocumentIdAndStatus(documentType, documentId, InstanceStatus.PENDING)
                .ifPresent(instance -> {
                    taskRepository.findByInstanceIdAndStatus(instance.getId(), TaskStatus.PENDING)
                            .forEach(task -> {
                                task.cancel();
                                taskRepository.save(task);
                            });
                    instance.cancel();
                    instanceRepository.save(instance);
                    historyRepository.save(
                            ApprovalHistoryEntry.of(instance.getTenantId(), instance.getId(), null, "INSTANCE_CANCELLED", null, null));
                });
    }

    private void createTasksForGroup(WorkflowInstance instance, List<ApprovalStep> groupSteps, UUID referenceUserId) {
        int groupOrder = groupSteps.get(0).getSequenceOrder();
        for (ApprovalStep step : groupSteps) {
            List<UUID> approvers = approverResolutionService.resolve(
                    step.getApproverType(),
                    instance.getCompanyId(),
                    step.getApproverRoleId(),
                    step.getApproverUserId(),
                    step.getHierarchyLevel(),
                    referenceUserId);
            if (approvers.isEmpty()) {
                throw new NoApproverResolvedException("Step '" + step.getName() + "' resolved to no eligible approvers");
            }
            Instant dueAt = step.getEscalationHours() == null
                    ? null
                    : Instant.now().plus(step.getEscalationHours(), ChronoUnit.HOURS);
            for (UUID approverId : approvers) {
                ApprovalTask task = taskRepository.save(
                        ApprovalTask.create(instance.getTenantId(), instance.getId(), step.getId(), step.getSequenceOrder(), approverId, dueAt));
                events.publishEvent(new WorkflowDecisionEvents.ApprovalRequested(
                        instance.getTenantId(),
                        instance.getCompanyId(),
                        instance.getDocumentType(),
                        instance.getDocumentId(),
                        instance.getId(),
                        task.getId(),
                        approverId,
                        Instant.now()));
            }
        }
        instance.advanceTo(groupOrder);
        instanceRepository.save(instance);
    }

    private void approveTask(ApprovalTask task, WorkflowInstance instance, UUID actingUserId, String comment) {
        task.approve(actingUserId, comment);
        taskRepository.save(task);
        historyRepository.save(ApprovalHistoryEntry.of(instance.getTenantId(), instance.getId(), task.getId(), "APPROVED", actingUserId, comment));

        // WF-3/WF-4: any one holder of a role step approving satisfies the whole step — the rest are moot.
        taskRepository.findByInstanceIdAndStepIdAndStatus(instance.getId(), task.getStepId(), TaskStatus.PENDING)
                .forEach(sibling -> {
                    sibling.cancel();
                    taskRepository.save(sibling);
                });

        long pendingInGroup =
                taskRepository.countByInstanceIdAndSequenceOrderAndStatus(instance.getId(), task.getSequenceOrder(), TaskStatus.PENDING);
        if (pendingInGroup > 0) {
            return;
        }

        List<Integer> laterGroups = planStepRepository.findByInstanceIdOrderBySequenceOrderAsc(instance.getId()).stream()
                .map(WorkflowInstanceStep::getSequenceOrder)
                .distinct()
                .filter(order -> order > task.getSequenceOrder())
                .sorted()
                .toList();

        if (laterGroups.isEmpty()) {
            instance.approve();
            instanceRepository.save(instance);
            historyRepository.save(
                    ApprovalHistoryEntry.of(instance.getTenantId(), instance.getId(), null, "INSTANCE_APPROVED", actingUserId, null));
            events.publishEvent(new WorkflowDecisionEvents.ApprovalGranted(
                    instance.getTenantId(), instance.getCompanyId(), instance.getDocumentType(), instance.getDocumentId(), instance.getId(), Instant.now()));
            return;
        }

        int nextGroup = laterGroups.get(0);
        List<UUID> nextStepIds = planStepRepository.findByInstanceIdAndSequenceOrder(instance.getId(), nextGroup).stream()
                .map(WorkflowInstanceStep::getStepId)
                .toList();
        List<ApprovalStep> nextSteps = stepRepository.findAllById(nextStepIds);
        createTasksForGroup(instance, nextSteps, instance.getSubmittedBy());
    }

    private void rejectTask(ApprovalTask task, WorkflowInstance instance, UUID actingUserId, String comment) {
        task.reject(actingUserId, comment);
        taskRepository.save(task);
        historyRepository.save(ApprovalHistoryEntry.of(instance.getTenantId(), instance.getId(), task.getId(), "REJECTED", actingUserId, comment));

        taskRepository.findByInstanceIdAndStatus(instance.getId(), TaskStatus.PENDING).forEach(pending -> {
            pending.cancel();
            taskRepository.save(pending);
        });

        instance.reject();
        instanceRepository.save(instance);
        historyRepository.save(
                ApprovalHistoryEntry.of(instance.getTenantId(), instance.getId(), null, "INSTANCE_REJECTED", actingUserId, comment));

        events.publishEvent(new WorkflowDecisionEvents.ApprovalRejected(
                instance.getTenantId(),
                instance.getCompanyId(),
                instance.getDocumentType(),
                instance.getDocumentId(),
                instance.getId(),
                actingUserId,
                comment,
                Instant.now()));
    }

    private boolean escalateOne(ApprovalTask task) {
        ApprovalStep step = stepRepository.findById(task.getStepId()).orElse(null);
        WorkflowInstance instance = instanceRepository.findById(task.getInstanceId()).orElse(null);
        if (step == null || instance == null || instance.getStatus() != InstanceStatus.PENDING) {
            return false;
        }

        List<UUID> targets = approverResolutionService.resolveEscalationTarget(step, instance.getCompanyId(), task.getAssignedUserId());
        if (targets.isEmpty()) {
            log.warn("WF-5: no escalation target resolved for task {} (step {}); leaving it pending", task.getId(), step.getId());
            return false;
        }

        task.escalate();
        taskRepository.save(task);
        historyRepository.save(ApprovalHistoryEntry.of(task.getTenantId(), instance.getId(), task.getId(), "ESCALATED", null, null));

        for (UUID target : targets) {
            ApprovalTask newTask = taskRepository.save(
                    ApprovalTask.create(task.getTenantId(), instance.getId(), step.getId(), step.getSequenceOrder(), target, null));
            events.publishEvent(new WorkflowDecisionEvents.TaskEscalated(
                    instance.getTenantId(),
                    instance.getCompanyId(),
                    instance.getDocumentType(),
                    instance.getDocumentId(),
                    instance.getId(),
                    task.getAssignedUserId(),
                    target,
                    newTask.getId(),
                    Instant.now()));
            events.publishEvent(new WorkflowDecisionEvents.ApprovalRequested(
                    instance.getTenantId(),
                    instance.getCompanyId(),
                    instance.getDocumentType(),
                    instance.getDocumentId(),
                    instance.getId(),
                    newTask.getId(),
                    target,
                    Instant.now()));
        }
        return true;
    }
}
