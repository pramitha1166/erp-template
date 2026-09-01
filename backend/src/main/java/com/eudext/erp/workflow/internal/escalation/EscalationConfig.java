package com.eudext.erp.workflow.internal.escalation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EscalationProperties.class)
class EscalationConfig {}
