package com.eudext.erp.config.document;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Wires {@code @CreatedBy}/{@code @LastModifiedBy} on {@link Document} (and
 * IAM's own audited entities) from the authenticated principal Epic 0.2's
 * {@code JwtAuthenticationFilter} sets on the security context — its name
 * is the user id, per that filter's javadoc. Falls back to {@code
 * "system"} for unauthenticated contexts (migrations, scheduled jobs,
 * tests) where there is no principal to read. Deliberately reads only
 * {@link Authentication#getName()} here rather than depending on any IAM
 * type: `config` is a shared, dependency-free module (ARCH-1) and must not
 * import from `iam`.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "documentAuditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> documentAuditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}
