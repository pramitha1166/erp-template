package com.eudext.erp.audit.internal.web;

import com.eudext.erp.audit.internal.archive.AuditArchiveStatusService;
import com.eudext.erp.audit.internal.archive.AuditArchiveStatusService.ArchiveStatus;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** AUD-5: read-only API behind the admin-only retention/archival status indicator (F0.3.3). */
@RestController
@RequestMapping("/audit/archive")
public class AuditArchiveStatusController {

    private final AuditArchiveStatusService statusService;
    private final AuditAccessGuard accessGuard;

    public AuditArchiveStatusController(AuditArchiveStatusService statusService, AuditAccessGuard accessGuard) {
        this.statusService = statusService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/status")
    public ArchiveStatus status(@RequestParam UUID companyId) {
        accessGuard.requirePermission(companyId);
        return statusService.statusFor(accessGuard.tenantId());
    }
}
