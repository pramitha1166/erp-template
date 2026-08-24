package com.eudext.erp.architecture;

import com.eudext.erp.ErpApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * ARCH-1: Spring Modulith modules may only talk to each other via published
 * events or the types explicitly exposed from a module's root package —
 * never by reaching into another module's internal packages or repositories.
 * {@link ApplicationModules#verify()} statically checks exactly that,
 * including the absence of cyclic module dependencies. A failing assertion
 * here means the design needs to change, not that this test needs relaxing.
 */
class ModularityTests {

    static final ApplicationModules MODULES = ApplicationModules.of(ErpApplication.class);

    @Test
    void moduleStructureRespectsBoundaries() {
        MODULES.verify();
    }
}
