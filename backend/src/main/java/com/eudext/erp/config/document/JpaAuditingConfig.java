package com.eudext.erp.config.document;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Wires {@code @CreatedBy}/{@code @LastModifiedBy} on {@link Document}.
 * Epic 0.2 (IAM) hasn't landed yet, so there is no authenticated principal
 * to read — this is a placeholder that always attributes to {@code
 * "system"}. Deliberately not pulling in {@code spring-security} to read a
 * "current user" here: that dependency belongs to Epic 0.2, and adding it
 * unconfigured would silently turn on Spring Boot's default HTTP Basic
 * auto-configuration for every endpoint. Once IAM-1 lands, replace the
 * bean body with a read of the authenticated principal.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "documentAuditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> documentAuditorAware() {
        return () -> Optional.of("system");
    }
}
