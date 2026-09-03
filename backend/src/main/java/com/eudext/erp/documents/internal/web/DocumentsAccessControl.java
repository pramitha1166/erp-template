package com.eudext.erp.documents.internal.web;

import com.eudext.erp.documents.internal.DocumentType;
import com.eudext.erp.iam.AuthenticationFailedException;
import com.eudext.erp.iam.PermissionApi;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * DOC-5: attachment/print-format access inherits the parent document's own permission checks. Unlike every other
 * module's {@code *AccessControl} (which check a hardcoded permission code — see e.g. {@code
 * com.eudext.erp.numbering.internal.web.NumberingAccessControl}), this one derives the code from the caller-supplied
 * {@code documentType}: acting on an attachment/print format of a {@code sales:invoice} requires exactly the
 * permission a {@code sales:invoice} itself would (e.g. {@code sales:invoice:view}), never a documents-module-owned
 * permission of its own.
 */
@Component
public class DocumentsAccessControl {

    private final PermissionApi permissionApi;

    public DocumentsAccessControl(PermissionApi permissionApi) {
        this.permissionApi = permissionApi;
    }

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AuthenticationFailedException("Not authenticated");
        }
        return UUID.fromString(authentication.getName());
    }

    /**
     * Returns the current user id if they hold {@code documentType + ":" + action} in {@code companyId}, otherwise
     * throws 403.
     */
    public UUID requireDocumentPermission(UUID companyId, String documentType, String action) {
        UUID userId = currentUserId();
        String permissionCode = DocumentType.permissionCode(documentType, action);
        if (!permissionApi.hasPermission(userId, companyId, permissionCode)) {
            throw new AccessDeniedException("Missing permission: " + permissionCode);
        }
        return userId;
    }
}
