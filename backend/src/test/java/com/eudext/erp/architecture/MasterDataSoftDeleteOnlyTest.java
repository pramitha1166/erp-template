package com.eudext.erp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;

/**
 * MDM-10: all master data is soft-delete only — a record is disabled, never hard-deleted, once it may be
 * referenced. Rather than trust every future controller to remember that, this enforces it structurally: no REST
 * endpoint under {@code com.eudext.erp.masterdata} may be a {@code @DeleteMapping} at all. A failing assertion here
 * means the new endpoint needs to be a disable/enable toggle instead, not that this rule needs relaxing.
 */
class MasterDataSoftDeleteOnlyTest {

    private static final JavaClasses MASTERDATA_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.eudext.erp.masterdata");

    @Test
    void noMasterDataEndpointIsAHardDelete() {
        ArchRule rule = noMethods()
                .should()
                .beAnnotatedWith(DeleteMapping.class)
                .because("MDM-10: master data is soft-delete only — expose a disable/enable toggle instead of a DELETE endpoint");
        rule.check(MASTERDATA_CLASSES);
    }
}
