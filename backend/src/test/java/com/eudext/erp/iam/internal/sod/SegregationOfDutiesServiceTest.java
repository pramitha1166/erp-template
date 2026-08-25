package com.eudext.erp.iam.internal.sod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegregationOfDutiesServiceTest {

    @Mock
    private SodRuleRepository repository;

    private SegregationOfDutiesService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new SegregationOfDutiesService(repository);
    }

    @Test
    void normalizesPairOrderingAlphabetically() {
        ArgumentCaptor<SodRule> captor = ArgumentCaptor.forClass(SodRule.class);
        when(repository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.createRule(tenantId, "procurement:supplier:create", "finance:payment:approve", "classic SoD conflict");

        assertThat(captor.getValue().getPermissionCodeA()).isEqualTo("finance:payment:approve");
        assertThat(captor.getValue().getPermissionCodeB()).isEqualTo("procurement:supplier:create");
    }

    @Test
    void rejectsPairingAPermissionWithItself() {
        assertThatThrownBy(() -> service.createRule(tenantId, "finance:payment:approve", "finance:payment:approve", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noConflictWhenSetDoesNotContainBothSidesOfAnyActiveRule() {
        SodRule rule = SodRule.create(tenantId, "a:x:y", "b:x:y", null);
        when(repository.findByActiveTrue()).thenReturn(List.of(rule));

        service.assertNoConflict(Set.of("a:x:y", "c:x:y"));
    }

    @Test
    void throwsWhenSetContainsBothSidesOfAnActiveRule() {
        SodRule rule = SodRule.create(tenantId, "a:x:y", "b:x:y", null);
        when(repository.findByActiveTrue()).thenReturn(List.of(rule));

        assertThatThrownBy(() -> service.assertNoConflict(Set.of("a:x:y", "b:x:y")))
                .isInstanceOf(SegregationOfDutiesViolationException.class);
    }

    @Test
    void inactiveRulesAreNotEnforced() {
        when(repository.findByActiveTrue()).thenReturn(List.of());

        service.assertNoConflict(Set.of("a:x:y", "b:x:y"));
    }
}
