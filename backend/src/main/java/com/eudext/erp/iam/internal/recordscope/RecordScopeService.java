package com.eudext.erp.iam.internal.recordscope;

import com.eudext.erp.iam.RecordScopeType;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** IAM-6: admin-facing CRUD for a role's record-level scope restrictions. */
@Service
public class RecordScopeService {

    private final RecordScopeRestrictionRepository repository;

    public RecordScopeService(RecordScopeRestrictionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void addRestriction(UUID tenantId, UUID roleId, RecordScopeType scopeType, UUID scopeValue) {
        repository.save(RecordScopeRestriction.of(tenantId, roleId, scopeType, scopeValue));
    }

    @Transactional
    public void removeRestriction(UUID restrictionId) {
        repository.deleteById(restrictionId);
    }

    @Transactional(readOnly = true)
    public List<RecordScopeRestriction> listForRole(UUID roleId) {
        return repository.findByRoleId(roleId);
    }
}
