package com.eudext.erp.audit.internal.archive;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditArchiveWatermarkRepository extends JpaRepository<AuditArchiveWatermark, UUID> {}
