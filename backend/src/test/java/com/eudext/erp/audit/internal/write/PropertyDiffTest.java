package com.eudext.erp.audit.internal.write;

import static org.assertj.core.api.Assertions.assertThat;

import com.eudext.erp.config.audit.AuditRedacted;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PropertyDiffTest {

    static class Sample {
        String name;

        @AuditRedacted
        String secret;
    }

    private static final String[] PROPERTY_NAMES = {"name", "secret"};

    @Test
    void allAsNewCapturesEveryPropertyForAnInsert() {
        Object[] state = {"Acme", "hunter2"};

        Map<String, Object> values = PropertyDiff.allAsNew(Sample.class, PROPERTY_NAMES, state);

        assertThat(values).containsEntry("name", "Acme").containsEntry("secret", PropertyDiff.REDACTED_MARKER);
    }

    @Test
    void allAsOldCapturesEveryPropertyForADelete() {
        Object[] state = {"Acme", "hunter2"};

        Map<String, Object> values = PropertyDiff.allAsOld(Sample.class, PROPERTY_NAMES, state);

        assertThat(values).containsEntry("name", "Acme").containsEntry("secret", PropertyDiff.REDACTED_MARKER);
    }

    @Test
    void changedOnlyReportsPropertiesThatActuallyDiffer() {
        Object[] previous = {"Acme", "hunter2"};
        Object[] current = {"Acme Corp", "hunter2"};

        PropertyDiff.Changed diff = PropertyDiff.changed(Sample.class, PROPERTY_NAMES, previous, current);

        assertThat(diff.oldValues()).containsExactly(Map.entry("name", "Acme"));
        assertThat(diff.newValues()).containsExactly(Map.entry("name", "Acme Corp"));
    }

    @Test
    void changedRedactsBothSidesOfARedactedFieldChange() {
        Object[] previous = {"Acme", "old-secret"};
        Object[] current = {"Acme", "new-secret"};

        PropertyDiff.Changed diff = PropertyDiff.changed(Sample.class, PROPERTY_NAMES, previous, current);

        assertThat(diff.oldValues()).containsEntry("secret", PropertyDiff.REDACTED_MARKER);
        assertThat(diff.newValues()).containsEntry("secret", PropertyDiff.REDACTED_MARKER);
    }

    @Test
    void changedIsEmptyWhenNothingDiffers() {
        Object[] state = {"Acme", "hunter2"};

        PropertyDiff.Changed diff = PropertyDiff.changed(Sample.class, PROPERTY_NAMES, state, state);

        assertThat(diff.isEmpty()).isTrue();
    }

    @Test
    void changedTreatsNullPreviousStateAsEveryPropertyBeingNew() {
        Object[] current = {"Acme", "hunter2"};

        PropertyDiff.Changed diff = PropertyDiff.changed(Sample.class, PROPERTY_NAMES, null, current);

        assertThat(diff.oldValues()).containsEntry("name", null);
        assertThat(diff.newValues()).containsEntry("name", "Acme");
    }
}
