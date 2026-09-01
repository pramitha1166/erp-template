package com.eudext.erp.workflow.internal.notify;

import com.eudext.erp.iam.IdentityProvisioningApi;
import com.eudext.erp.notification.NotificationApi;
import com.eudext.erp.workflow.WorkflowDecisionEvents;
import java.util.Map;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * WF-8: the email leg of "email + in-app notification on pending
 * approval" — notifies whichever user a task is (or becomes, via WF-5
 * escalation) newly assigned to. The in-app leg is served directly from
 * workflow's own pending-tasks query (see {@code ApprovalTaskController}'s
 * {@code /workflow/tasks/mine}) rather than through {@code notification}'s
 * outbox, which has no per-user inbox concept (it's an outbound send log
 * keyed by email address, not userId).
 *
 * <p>{@code @ApplicationModuleListener} runs after the publishing
 * transaction commits, so a rolled-back approval step never sends a
 * notification — same guarantee {@code AuthAuditEventListener} relies on
 * for IAM-10.
 */
@Component
class WorkflowNotificationListener {

    private final NotificationApi notificationApi;
    private final IdentityProvisioningApi identityProvisioningApi;

    WorkflowNotificationListener(NotificationApi notificationApi, IdentityProvisioningApi identityProvisioningApi) {
        this.notificationApi = notificationApi;
        this.identityProvisioningApi = identityProvisioningApi;
    }

    @ApplicationModuleListener
    void on(WorkflowDecisionEvents.ApprovalRequested event) {
        notificationApi.send(
                event.tenantId(),
                identityProvisioningApi.emailOf(event.assignedUserId()),
                "workflow-approval-pending",
                Map.of(
                        "documentType", event.documentType(),
                        "documentId", event.documentId().toString(),
                        "instanceId", event.instanceId().toString()));
    }
}
