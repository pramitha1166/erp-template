package com.eudext.erp.iam.internal.auth;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Reads the user id {@link JwtAuthenticationFilter} placed on the security context for this request. */
@Component
public class CurrentUserResolver {

    public UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new AuthenticationFailedException("Not authenticated");
        }
        return UUID.fromString(authentication.getName());
    }
}
