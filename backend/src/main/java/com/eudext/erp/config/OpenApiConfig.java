package com.eudext.erp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * NFR-M4: OpenAPI generated from code. Title is intentionally generic
 * (BRD-2) — no brand name in source outside the seed data file.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI erpOpenApi() {
        return new OpenAPI().info(new Info().title("ERP Platform API").version("v1"));
    }
}
