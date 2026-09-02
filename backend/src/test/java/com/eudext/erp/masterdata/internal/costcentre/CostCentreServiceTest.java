package com.eudext.erp.masterdata.internal.costcentre;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** MDM-4: hierarchical cost centre CRUD. */
@ExtendWith(MockitoExtension.class)
class CostCentreServiceTest {

    @Mock
    private CostCentreRepository repository;

    private CostCentreService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CostCentreService(repository);
    }

    @Test
    void createsATopLevelCostCentreWithNoParent() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CostCentre costCentre = service.create(tenantId, companyId, "HO", "Head Office", null);

        assertThat(costCentre.getParentId()).isNull();
        assertThat(costCentre.getCode()).isEqualTo("HO");
    }

    @Test
    void rejectsACreateUnderANonExistentParent() {
        UUID parentId = UUID.randomUUID();
        when(repository.existsById(parentId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(tenantId, companyId, "BR1", "Branch 1", parentId))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void disableAndEnableToggleTheDisabledFlag() {
        CostCentre costCentre = CostCentre.create(tenantId, companyId, "HO", "Head Office", null);
        UUID id = UUID.randomUUID();
        setId(costCentre, id);
        when(repository.findById(id)).thenReturn(Optional.of(costCentre));

        service.disable(id);
        assertThat(costCentre.isDisabled()).isTrue();

        service.enable(id);
        assertThat(costCentre.isDisabled()).isFalse();
    }

    private static void setId(CostCentre costCentre, UUID id) {
        try {
            var idField = CostCentre.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(costCentre, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
