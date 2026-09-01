package com.eudext.erp.workflow.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.workflow.internal.delegation.ApprovalDelegation;
import com.eudext.erp.workflow.internal.delegation.DelegationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** WF-5: a user delegating their own approval authority for a date range. Always self-service — a user may only create or revoke their own delegations. */
@RestController
@RequestMapping("/workflow/delegations")
public class ApprovalDelegationController {

    private final DelegationService delegationService;
    private final WorkflowAccessControl accessControl;

    public ApprovalDelegationController(DelegationService delegationService, WorkflowAccessControl accessControl) {
        this.delegationService = delegationService;
        this.accessControl = accessControl;
    }

    public record CreateDelegationRequest(
            @NotNull UUID delegateUserId, @NotNull LocalDate startDate, @NotNull LocalDate endDate, String reason) {}

    public record DelegationView(
            UUID id, UUID delegatorUserId, UUID delegateUserId, LocalDate startDate, LocalDate endDate, String reason, boolean revoked) {
        static DelegationView from(ApprovalDelegation delegation) {
            return new DelegationView(
                    delegation.getId(),
                    delegation.getDelegatorUserId(),
                    delegation.getDelegateUserId(),
                    delegation.getStartDate(),
                    delegation.getEndDate(),
                    delegation.getReason(),
                    delegation.isRevoked());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DelegationView delegate(@Valid @RequestBody CreateDelegationRequest request) {
        UUID delegatorUserId = accessControl.currentUserId();
        return DelegationView.from(delegationService.delegate(
                tenantId(), delegatorUserId, request.delegateUserId(), request.startDate(), request.endDate(), request.reason()));
    }

    @GetMapping("/mine")
    public List<DelegationView> myDelegations() {
        return delegationService.delegationsOf(accessControl.currentUserId()).stream().map(DelegationView::from).toList();
    }

    @DeleteMapping("/{delegationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID delegationId) {
        delegationService.revoke(delegationId, accessControl.currentUserId());
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
