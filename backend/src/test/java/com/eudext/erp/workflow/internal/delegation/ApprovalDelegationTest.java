package com.eudext.erp.workflow.internal.delegation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** WF-5: date-ranged delegation of approval authority. */
class ApprovalDelegationTest {

    private final UUID tenantId = UUID.randomUUID();
    private final UUID delegator = UUID.randomUUID();
    private final UUID delegate = UUID.randomUUID();

    @Test
    void isActiveWithinInclusiveDateRange() {
        ApprovalDelegation delegation = ApprovalDelegation.create(
                tenantId, delegator, delegate, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20), "leave");

        assertThat(delegation.isActiveOn(LocalDate.of(2026, 1, 9))).isFalse();
        assertThat(delegation.isActiveOn(LocalDate.of(2026, 1, 10))).isTrue();
        assertThat(delegation.isActiveOn(LocalDate.of(2026, 1, 15))).isTrue();
        assertThat(delegation.isActiveOn(LocalDate.of(2026, 1, 20))).isTrue();
        assertThat(delegation.isActiveOn(LocalDate.of(2026, 1, 21))).isFalse();
    }

    @Test
    void revokedDelegationIsNeverActive() {
        ApprovalDelegation delegation = ApprovalDelegation.create(
                tenantId, delegator, delegate, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null);
        delegation.revoke();

        assertThat(delegation.isActiveOn(LocalDate.of(2026, 6, 1))).isFalse();
    }

    @Test
    void cannotDelegateToSelf() {
        assertThatThrownBy(() -> ApprovalDelegation.create(
                        tenantId, delegator, delegator, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void endDateCannotPrecedeStartDate() {
        assertThatThrownBy(() -> ApprovalDelegation.create(
                        tenantId, delegator, delegate, LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
