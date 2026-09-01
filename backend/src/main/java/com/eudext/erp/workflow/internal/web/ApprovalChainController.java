package com.eudext.erp.workflow.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.workflow.internal.chain.ApprovalChain;
import com.eudext.erp.workflow.internal.chain.ApprovalChainService;
import com.eudext.erp.workflow.internal.chain.ApprovalStep;
import com.eudext.erp.workflow.internal.chain.ApprovalStepCondition;
import com.eudext.erp.workflow.internal.chain.ApproverType;
import com.eudext.erp.workflow.internal.chain.ConditionOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** WF-1 / WF-2 / WF-3: approval chain configuration — chains, their steps, and step conditions. */
@RestController
@RequestMapping("/workflow/chains")
public class ApprovalChainController {

    private static final String PERMISSION_MANAGE = "workflow:approval-chain:manage";
    private static final String PERMISSION_VIEW = "workflow:approval-chain:view";

    private final ApprovalChainService chainService;
    private final WorkflowAccessControl accessControl;

    public ApprovalChainController(ApprovalChainService chainService, WorkflowAccessControl accessControl) {
        this.chainService = chainService;
        this.accessControl = accessControl;
    }

    public record CreateChainRequest(@NotBlank String documentType, @NotBlank String name) {}

    public record ChainView(UUID id, UUID companyId, String documentType, String name, boolean active) {
        static ChainView from(ApprovalChain chain) {
            return new ChainView(chain.getId(), chain.getCompanyId(), chain.getDocumentType(), chain.getName(), chain.isActive());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChainView createChain(@RequestParam UUID companyId, @Valid @RequestBody CreateChainRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return ChainView.from(chainService.createChain(tenantId(), companyId, request.documentType(), request.name()));
    }

    @PostMapping("/{chainId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable UUID chainId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        chainService.activate(chainId);
    }

    @PostMapping("/{chainId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID chainId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        chainService.deactivate(chainId);
    }

    @GetMapping
    public List<ChainView> listChains(@RequestParam UUID companyId, @RequestParam String documentType) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return chainService.chainsFor(companyId, documentType).stream().map(ChainView::from).toList();
    }

    public record CreateStepRequest(
            int sequenceOrder,
            @NotBlank String name,
            @NotNull ApproverType approverType,
            UUID roleId,
            UUID userId,
            Integer hierarchyLevel) {}

    public record StepView(
            UUID id,
            int sequenceOrder,
            String name,
            ApproverType approverType,
            UUID approverRoleId,
            UUID approverUserId,
            Integer hierarchyLevel,
            Integer escalationHours) {
        static StepView from(ApprovalStep step) {
            return new StepView(
                    step.getId(),
                    step.getSequenceOrder(),
                    step.getName(),
                    step.getApproverType(),
                    step.getApproverRoleId(),
                    step.getApproverUserId(),
                    step.getHierarchyLevel(),
                    step.getEscalationHours());
        }
    }

    @PostMapping("/{chainId}/steps")
    @ResponseStatus(HttpStatus.CREATED)
    public StepView addStep(@PathVariable UUID chainId, @RequestParam UUID companyId, @Valid @RequestBody CreateStepRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        ApprovalStep step =
                switch (request.approverType()) {
                    case ROLE -> chainService.addRoleStep(
                            tenantId(), chainId, request.sequenceOrder(), request.name(), require(request.roleId(), "roleId"));
                    case USER -> chainService.addUserStep(
                            tenantId(), chainId, request.sequenceOrder(), request.name(), require(request.userId(), "userId"));
                    case HIERARCHY -> chainService.addHierarchyStep(
                            tenantId(),
                            chainId,
                            request.sequenceOrder(),
                            request.name(),
                            require(request.hierarchyLevel(), "hierarchyLevel"));
                };
        return StepView.from(step);
    }

    private static <T> T require(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required for this approverType");
        }
        return value;
    }

    public record EscalationRequest(
            @NotNull Integer hours, ApproverType escalationType, UUID roleId, UUID userId, Integer hierarchyLevel) {}

    @PostMapping("/{chainId}/steps/{stepId}/escalation")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void configureEscalation(
            @PathVariable UUID chainId,
            @PathVariable UUID stepId,
            @RequestParam UUID companyId,
            @Valid @RequestBody EscalationRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        chainService.configureEscalation(
                stepId, request.hours(), request.escalationType(), request.roleId(), request.userId(), request.hierarchyLevel());
    }

    @GetMapping("/{chainId}/steps")
    public List<StepView> listSteps(@PathVariable UUID chainId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return chainService.stepsOf(chainId).stream().map(StepView::from).toList();
    }

    public record CreateConditionRequest(
            @NotBlank String fieldName, @NotNull ConditionOperator operator, String valueString, BigDecimal valueNumber) {}

    public record ConditionView(UUID id, String fieldName, ConditionOperator operator, String valueString, BigDecimal valueNumber) {
        static ConditionView from(ApprovalStepCondition condition) {
            return new ConditionView(
                    condition.getId(),
                    condition.getFieldName(),
                    condition.getOperator(),
                    condition.getValueString(),
                    condition.getValueNumber());
        }
    }

    @PostMapping("/{chainId}/steps/{stepId}/conditions")
    @ResponseStatus(HttpStatus.CREATED)
    public ConditionView addCondition(
            @PathVariable UUID chainId,
            @PathVariable UUID stepId,
            @RequestParam UUID companyId,
            @Valid @RequestBody CreateConditionRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        ApprovalStepCondition condition = request.valueNumber() != null
                ? chainService.addNumberCondition(tenantId(), stepId, request.fieldName(), request.operator(), request.valueNumber())
                : chainService.addTextCondition(tenantId(), stepId, request.fieldName(), request.operator(), request.valueString());
        return ConditionView.from(condition);
    }

    @GetMapping("/steps/{stepId}/conditions")
    public List<ConditionView> listConditions(@PathVariable UUID stepId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return chainService.conditionsOf(stepId).stream().map(ConditionView::from).toList();
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
