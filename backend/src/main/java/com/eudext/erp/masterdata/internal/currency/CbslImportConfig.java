package com.eudext.erp.masterdata.internal.currency;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CbslImportProperties.class)
class CbslImportConfig {}
