package com.eudext.erp.audit.internal.write;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Plugs {@link AuditingInterceptor} into Hibernate's session factory via
 * the {@code hibernate.session_factory.interceptor} property — the
 * standard Spring Boot recipe for a custom {@code org.hibernate.Interceptor},
 * since Boot doesn't auto-detect one from a bean definition the way it does
 * {@code PhysicalNamingStrategy} or similar.
 */
@Configuration
class AuditInterceptorConfig {

    @Bean
    AuditingInterceptor auditingInterceptor(AuditLogWriter writer) {
        return new AuditingInterceptor(writer);
    }

    @Bean
    HibernatePropertiesCustomizer auditInterceptorCustomizer(AuditingInterceptor interceptor) {
        return properties -> properties.put("hibernate.session_factory.interceptor", interceptor);
    }
}
