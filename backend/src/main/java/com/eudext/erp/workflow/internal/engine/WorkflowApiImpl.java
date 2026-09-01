package com.eudext.erp.workflow.internal.engine;

import com.eudext.erp.workflow.WorkflowApi;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class WorkflowApiImpl implements WorkflowApi {

    private final WorkflowEngine engine;

    WorkflowApiImpl(WorkflowEngine engine) {
        this.engine = engine;
    }

    @Override
    public Outcome startApproval(StartApprovalRequest request) {
        return engine.startApproval(request);
    }

    @Override
    public Status statusOf(String documentType, UUID documentId) {
        return engine.statusOf(documentType, documentId);
    }

    @Override
    public void cancelPending(String documentType, UUID documentId) {
        engine.cancelPending(documentType, documentId);
    }
}
