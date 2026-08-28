/**
 * Platform and Brand Administration module (PLAT-ADMIN, SRS §3.10,
 * ADM-1..ADM-9). Spring Modulith application module — collaborates with
 * other modules only via published events or the types exposed from this
 * package; internal implementation lives under {@code internal}
 * sub-packages (ARCH-1).
 *
 * <p>Owns the {@code Brand} and {@code Tenant} registry that Epic 0.8
 * (Branding) and Epic 0.9 (Org dimensions) will later extend — see the gap
 * this epic was added to close in {@code docs/SRS.md} §3.10: nothing in
 * v1.0 defined who provisions a Brand or how a Tenant actually gets
 * onboarded, and this module is that missing piece.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Platform and Brand Administration")
package com.eudext.erp.admin;
