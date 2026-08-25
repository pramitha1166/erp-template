package com.eudext.erp.config.tenancy;

import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/** Binds the tenant-aware {@code DataSource} bean to {@link TenantContextScope} once at startup — see that class's javadoc. */
@Component
class TenantContextScopeInitializer {

    TenantContextScopeInitializer(DataSource dataSource) {
        TenantContextScope.bind(dataSource);
    }
}
