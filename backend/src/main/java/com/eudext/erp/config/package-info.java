/**
 * Cross-cutting platform configuration (logging, correlation ids, web
 * filters) shared by every module. Declared {@code OPEN} so it has none of
 * the module-boundary restrictions ARCH-1 imposes on domain modules.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Platform Configuration",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.eudext.erp.config;
