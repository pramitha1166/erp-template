package com.eudext.erp.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * ARCH-5: {@code double}/{@code float} are prohibited everywhere in
 * production code, not just on fields that are obviously monetary — the
 * SRS calls for a static-analysis rule that fails the build outright rather
 * than one that has to be trusted to correctly guess which fields are
 * money. Monetary values must use {@code BigDecimal} at scale 4.
 */
class MonetaryPrimitiveTypeTest {

    private static final Set<String> BANNED_TYPES =
            Set.of("double", "float", Double.class.getName(), Float.class.getName());

    private static final JavaClasses PRODUCTION_CLASSES =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.eudext.erp");

    @Test
    void noDoubleOrFloatAnywhereInProductionCode() {
        ArchRule rule =
                classes()
                        .should(notDeclareDoubleOrFloat())
                        .because(
                                "ARCH-5: monetary values must be BigDecimal at scale 4; "
                                        + "double/float are banned outright, build-wide");
        rule.check(PRODUCTION_CLASSES);
    }

    private static ArchCondition<JavaClass> notDeclareDoubleOrFloat() {
        return new ArchCondition<JavaClass>("not declare any field, parameter, or return type as double/float") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaField field : javaClass.getFields()) {
                    if (BANNED_TYPES.contains(field.getRawType().getName())) {
                        events.add(SimpleConditionEvent.violated(
                                field, field.getFullName() + " has banned type " + field.getRawType().getName()));
                    }
                }
                for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
                    if (BANNED_TYPES.contains(codeUnit.getRawReturnType().getName())) {
                        events.add(SimpleConditionEvent.violated(
                                codeUnit,
                                codeUnit.getFullName()
                                        + " returns banned type "
                                        + codeUnit.getRawReturnType().getName()));
                    }
                    codeUnit.getParameters().forEach(parameter -> {
                        if (BANNED_TYPES.contains(parameter.getRawType().getName())) {
                            events.add(SimpleConditionEvent.violated(
                                    codeUnit,
                                    codeUnit.getFullName()
                                            + " has a parameter of banned type "
                                            + parameter.getRawType().getName()));
                        }
                    });
                }
            }
        };
    }
}
