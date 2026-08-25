package com.eudext.erp.iam.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.internal.auth.AccessControlService;
import com.eudext.erp.iam.internal.sod.SegregationOfDutiesService;
import com.eudext.erp.iam.internal.sod.SodRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** IAM-7: configurable Segregation-of-Duties rules. */
@RestController
@RequestMapping("/iam/sod-rules")
public class SodRuleController {

    private static final String PERMISSION_MANAGE_SOD = "iam:sod-rule:manage";

    private final SegregationOfDutiesService segregationOfDutiesService;
    private final AccessControlService accessControlService;

    public SodRuleController(SegregationOfDutiesService segregationOfDutiesService, AccessControlService accessControlService) {
        this.segregationOfDutiesService = segregationOfDutiesService;
        this.accessControlService = accessControlService;
    }

    public record CreateSodRuleRequest(@NotBlank String permissionCodeA, @NotBlank String permissionCodeB, String description) {}

    public record SodRuleView(UUID id, String permissionCodeA, String permissionCodeB, String description, boolean active) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SodRuleView create(@RequestParam UUID companyId, @Valid @RequestBody CreateSodRuleRequest request) {
        accessControlService.requirePermission(companyId, PERMISSION_MANAGE_SOD);
        UUID tenantId = TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
        SodRule rule = segregationOfDutiesService.createRule(
                tenantId, request.permissionCodeA(), request.permissionCodeB(), request.description());
        return toView(rule);
    }

    @GetMapping
    public List<SodRuleView> list() {
        return segregationOfDutiesService.listAll().stream().map(this::toView).toList();
    }

    @DeleteMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID ruleId, @RequestParam UUID companyId) {
        accessControlService.requirePermission(companyId, PERMISSION_MANAGE_SOD);
        segregationOfDutiesService.delete(ruleId);
    }

    private SodRuleView toView(SodRule rule) {
        return new SodRuleView(rule.getId(), rule.getPermissionCodeA(), rule.getPermissionCodeB(), rule.getDescription(), rule.isActive());
    }
}
