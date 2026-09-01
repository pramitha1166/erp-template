package com.eudext.erp.workflow.internal.chain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalChainRepository extends JpaRepository<ApprovalChain, UUID> {

    Optional<ApprovalChain> findByCompanyIdAndDocumentTypeAndActiveTrue(UUID companyId, String documentType);

    List<ApprovalChain> findByCompanyIdAndDocumentType(UUID companyId, String documentType);
}
