package com.eudext.erp.audit.internal.write;

import com.eudext.erp.audit.internal.log.AuditAction;
import com.eudext.erp.config.audit.NotAudited;
import com.eudext.erp.config.tenancy.TenantContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * AUD-1 / 0.3.3: the generic mechanism — every {@code @Entity} in every
 * module gets audited automatically on insert/update/delete, with no
 * per-module boilerplate, by hooking Hibernate's own flush lifecycle
 * rather than requiring each service to call an audit API. Registered
 * against the whole session factory by {@code AuditInterceptorConfig}.
 *
 * <p>Changes are only buffered here, not written — see {@link
 * AuditLogWriter} for why the actual persistence happens on a completely
 * separate path after the owning transaction commits, via {@link
 * TransactionSynchronization#afterCommit()}. Buffering is per-thread
 * ({@link ThreadLocal}), which is safe because Hibernate flush and the
 * Spring-managed transaction it participates in run on the same thread in
 * this codebase's synchronous request-per-thread model.
 */
public class AuditingInterceptor implements Interceptor {

    private static final ThreadLocal<List<PendingAuditEntry>> BUFFER = new ThreadLocal<>();

    private final AuditLogWriter writer;

    public AuditingInterceptor(AuditLogWriter writer) {
        this.writer = writer;
    }

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        if (isAudited(entity.getClass())) {
            buffer(
                    entity,
                    id,
                    AuditAction.INSERT,
                    Map.of(),
                    PropertyDiff.allAsNew(entity.getClass(), propertyNames, state));
        }
        return false;
    }

    @Override
    public boolean onFlushDirty(
            Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        if (isAudited(entity.getClass())) {
            PropertyDiff.Changed diff = PropertyDiff.changed(entity.getClass(), propertyNames, previousState, currentState);
            if (!diff.isEmpty()) {
                buffer(entity, id, AuditAction.UPDATE, diff.oldValues(), diff.newValues());
            }
        }
        return false;
    }

    @Override
    public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) {
        if (isAudited(entity.getClass())) {
            buffer(
                    entity,
                    id,
                    AuditAction.DELETE,
                    PropertyDiff.allAsOld(entity.getClass(), propertyNames, state),
                    Map.of());
        }
    }

    private void buffer(
            Object entity, Object id, AuditAction action, Map<String, Object> oldValues, Map<String, Object> newValues) {
        PendingAuditEntry entry = new PendingAuditEntry(
                TenantContext.get().orElse(null),
                entity.getClass().getSimpleName(),
                String.valueOf(id),
                action,
                oldValues,
                newValues,
                currentActor(),
                AuditRequestContext.currentIpAddress(),
                AuditRequestContext.currentRequestId(),
                Instant.now());
        List<PendingAuditEntry> pending = BUFFER.get();
        if (pending == null) {
            pending = new ArrayList<>();
            BUFFER.set(pending);
            if (!registerFlushOnCommit(pending)) {
                BUFFER.remove();
                return;
            }
        }
        pending.add(entry);
    }

    /**
     * Returns {@code false} (and expects the caller to drop the buffer) when
     * there is no Spring-managed transaction to hang the write on — e.g. an
     * unmanaged save outside any {@code @Transactional} boundary. Writing
     * immediately there would be indistinguishable from writing before the
     * business change is known to have committed, so it's dropped rather
     * than risked.
     */
    private boolean registerFlushOnCommit(List<PendingAuditEntry> pending) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pending.forEach(writer::write);
            }

            @Override
            public void afterCompletion(int status) {
                BUFFER.remove();
            }
        });
        return true;
    }

    private static boolean isAudited(Class<?> entityType) {
        if (entityType.getPackageName().startsWith("com.eudext.erp.audit")) {
            return false;
        }
        return !entityType.isAnnotationPresent(NotAudited.class);
    }

    private static String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null || authentication.getName() == null ? "system" : authentication.getName();
    }
}
