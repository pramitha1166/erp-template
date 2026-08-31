package com.eudext.erp.admin.internal.impersonation;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpersonationSessionRepository extends JpaRepository<ImpersonationSession, UUID> {}
