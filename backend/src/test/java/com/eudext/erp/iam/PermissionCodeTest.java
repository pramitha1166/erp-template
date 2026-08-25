package com.eudext.erp.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PermissionCodeTest {

    @Test
    void parsesAWellFormedTriple() {
        PermissionCode code = PermissionCode.parse("finance:journal-entry:submit");
        assertThat(code.module()).isEqualTo("finance");
        assertThat(code.entity()).isEqualTo("journal-entry");
        assertThat(code.action()).isEqualTo("submit");
        assertThat(code.toString()).isEqualTo("finance:journal-entry:submit");
        assertThat(code.entityCode()).isEqualTo("finance:journal-entry");
    }

    @Test
    void rejectsWrongSegmentCount() {
        assertThatThrownBy(() -> PermissionCode.parse("finance:journal-entry")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PermissionCode.parse("finance:journal-entry:submit:extra"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsUppercaseOrInvalidCharacters() {
        assertThatThrownBy(() -> PermissionCode.parse("Finance:journal-entry:submit"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PermissionCode.parse("finance:journal_entry:submit"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
