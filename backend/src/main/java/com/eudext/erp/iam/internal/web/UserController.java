package com.eudext.erp.iam.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.iam.internal.auth.AccessControlService;
import com.eudext.erp.iam.internal.user.User;
import com.eudext.erp.iam.internal.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * IAM-1: user provisioning. Gated by {@code iam:user:create} — which, for
 * a brand-new tenant with no users yet, nobody can hold. Bootstrapping a
 * tenant's first user/admin role is not in this epic's scope; it belongs
 * with whichever epic builds tenant/company onboarding (Epic 0.9 or
 * later), the same way Company/Branch masters are deferred there.
 */
@RestController
@RequestMapping("/iam/users")
public class UserController {

    private static final String PERMISSION_CREATE_USER = "iam:user:create";

    private final UserService userService;
    private final AccessControlService accessControlService;

    public UserController(UserService userService, AccessControlService accessControlService) {
        this.userService = userService;
        this.accessControlService = accessControlService;
    }

    public record CreateUserRequest(@Email @NotBlank String email, @NotBlank String password) {}

    public record UserView(UUID id, String email) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserView createUser(@RequestParam UUID companyId, @Valid @RequestBody CreateUserRequest request) {
        accessControlService.requirePermission(companyId, PERMISSION_CREATE_USER);
        UUID tenantId = TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
        User user = userService.createUser(tenantId, request.email(), request.password());
        return new UserView(user.getId(), user.getEmail());
    }
}
