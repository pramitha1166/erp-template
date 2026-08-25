package com.eudext.erp.scheduler.internal;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on Spring's {@code @Scheduled} infrastructure for the whole
 * application — the Scheduler module exists precisely to own this
 * platform-wide switch, so individual modules with a scheduled job (e.g.
 * {@code audit}'s AUD-5 archival sweep) don't each have to enable it
 * themselves.
 */
@Configuration
@EnableScheduling
class SchedulingConfig {}
