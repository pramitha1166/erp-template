package com.eudext.erp.admin.internal.checklist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ADM-4: post-onboarding setup checklist seeding and manual completion tracking. */
@ExtendWith(MockitoExtension.class)
class ChecklistServiceTest {

    @Mock
    private OnboardingChecklistItemRepository repository;

    private ChecklistService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ChecklistService(repository);
    }

    @Test
    void seedDefaultsCreatesExactlyOneItemPerFixedKey() {
        when(repository.existsByTenantId(tenantId)).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> inv.getArgument(0));

        service.seedDefaults(tenantId);

        ArgumentCaptor<OnboardingChecklistItem> captor = ArgumentCaptor.forClass(OnboardingChecklistItem.class);
        verify(repository, times(ChecklistItemKey.values().length)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(OnboardingChecklistItem::getItemKey).containsExactly(ChecklistItemKey.values());
        assertThat(captor.getAllValues()).allSatisfy(item -> assertThat(item.isCompleted()).isFalse());
    }

    @Test
    void seedDefaultsIsANoOpIfAlreadySeeded() {
        when(repository.existsByTenantId(tenantId)).thenReturn(true);

        service.seedDefaults(tenantId);

        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void setCompletedTracksTheCompletionTimestamp() {
        OnboardingChecklistItem item = OnboardingChecklistItem.of(tenantId, ChecklistItemKey.FIRST_INVOICE);
        when(repository.findByTenantIdAndItemKey(tenantId, ChecklistItemKey.FIRST_INVOICE)).thenReturn(Optional.of(item));

        service.setCompleted(tenantId, ChecklistItemKey.FIRST_INVOICE, true);

        assertThat(item.isCompleted()).isTrue();
        assertThat(item.getCompletedAt()).isNotNull();
    }

    @Test
    void uncompletingClearsTheTimestamp() {
        OnboardingChecklistItem item = OnboardingChecklistItem.of(tenantId, ChecklistItemKey.FIRST_INVOICE);
        item.setCompleted(true);
        when(repository.findByTenantIdAndItemKey(tenantId, ChecklistItemKey.FIRST_INVOICE)).thenReturn(Optional.of(item));

        service.setCompleted(tenantId, ChecklistItemKey.FIRST_INVOICE, false);

        assertThat(item.isCompleted()).isFalse();
        assertThat(item.getCompletedAt()).isNull();
    }

    @Test
    void settingAnUnknownItemThrows() {
        when(repository.findByTenantIdAndItemKey(tenantId, ChecklistItemKey.BRANCHES)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setCompleted(tenantId, ChecklistItemKey.BRANCHES, true))
                .isInstanceOf(NoSuchElementException.class);
    }
}
