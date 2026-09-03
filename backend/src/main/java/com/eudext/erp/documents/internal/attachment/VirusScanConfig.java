package com.eudext.erp.documents.internal.attachment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.capybara.clamav.ClamavClient;

/** DOC-4. Only wired up when {@code eudext.documents.attachments.virus-scan.enabled=true} (off by default). */
@Configuration
@EnableConfigurationProperties(VirusScanProperties.class)
class VirusScanConfig {

    @Bean
    @ConditionalOnProperty(prefix = "eudext.documents.attachments.virus-scan", name = "enabled", havingValue = "true")
    ClamavClient clamavClient(VirusScanProperties properties) {
        return new ClamavClient(properties.getHost(), properties.getPort());
    }

    @Bean
    @ConditionalOnProperty(prefix = "eudext.documents.attachments.virus-scan", name = "enabled", havingValue = "true")
    VirusScanner virusScanner(ClamavClient clamavClient) {
        return new ClamAvVirusScanner(clamavClient);
    }
}
