package com.eudext.erp.config.tenancy;

import javax.sql.DataSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wraps the Boot-autoconfigured {@code DataSource} bean with {@link
 * TenantAwareDataSource} in place, so every consumer — JPA, and Flyway
 * (harmlessly: migrations run DDL, which RLS does not govern) — gets a
 * connection stamped with the current tenant per ARCH-2.
 *
 * <p>Done as a {@link BeanPostProcessor} on the single {@code dataSource}
 * bean rather than as a second {@code @Primary} bean depending on it:
 * with two same-typed beans, Spring resolves an unqualified {@code
 * DataSource} injection point to whichever one is {@code @Primary} before
 * it considers the parameter name — including the wrapper's own
 * constructor parameter, which self-references and fails to start.
 * Post-processing the existing bean in place has no such ambiguity.
 */
@Configuration
public class TenantDataSourceConfig {

    @Bean
    public static BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof DataSource dataSource && !(bean instanceof TenantAwareDataSource)) {
                    return new TenantAwareDataSource(dataSource);
                }
                return bean;
            }
        };
    }
}
