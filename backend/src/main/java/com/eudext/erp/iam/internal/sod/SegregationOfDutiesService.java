package com.eudext.erp.iam.internal.sod;

import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-7: configurable conflicting-permission-pair rules that block a role/permission assignment. */
@Service
public class SegregationOfDutiesService {

    private final SodRuleRepository repository;

    public SegregationOfDutiesService(SodRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SodRule createRule(UUID tenantId, String permissionCodeA, String permissionCodeB, String description) {
        String[] ordered = normalize(permissionCodeA, permissionCodeB);
        return repository.save(SodRule.create(tenantId, ordered[0], ordered[1], description));
    }

    @Transactional(readOnly = true)
    public java.util.List<SodRule> listAll() {
        return repository.findAll();
    }

    @Transactional
    public void setActive(UUID ruleId, boolean active) {
        SodRule rule = repository.findById(ruleId).orElseThrow();
        if (active) {
            rule.activate();
        } else {
            rule.deactivate();
        }
        repository.save(rule);
    }

    @Transactional
    public void delete(UUID ruleId) {
        repository.deleteById(ruleId);
    }

    /**
     * Checks whether {@code effectivePermissionCodes} — the full set of
     * permissions a user would hold in a company after an assignment goes
     * through — violates any active rule, and throws if so. Called before
     * the assignment is persisted, never after.
     */
    @Transactional(readOnly = true)
    public void assertNoConflict(Set<String> effectivePermissionCodes) {
        for (SodRule rule : repository.findByActiveTrue()) {
            if (effectivePermissionCodes.contains(rule.getPermissionCodeA())
                    && effectivePermissionCodes.contains(rule.getPermissionCodeB())) {
                throw new SegregationOfDutiesViolationException(rule.getPermissionCodeA(), rule.getPermissionCodeB());
            }
        }
    }

    private String[] normalize(String a, String b) {
        if (a.equals(b)) {
            throw new IllegalArgumentException("SoD rule cannot pair a permission with itself: " + a);
        }
        return a.compareTo(b) < 0 ? new String[] {a, b} : new String[] {b, a};
    }
}
