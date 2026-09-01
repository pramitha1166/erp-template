package com.eudext.erp.workflow.internal.instance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, UUID> {

    Optional<WorkflowInstance> findByDocumentTypeAndDocumentIdAndStatus(
            String documentType, UUID documentId, InstanceStatus status);

    List<WorkflowInstance> findByDocumentTypeAndDocumentIdOrderByCreatedAtAsc(String documentType, UUID documentId);
}
