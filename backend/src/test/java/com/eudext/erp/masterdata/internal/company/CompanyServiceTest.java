package com.eudext.erp.masterdata.internal.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** MDM-1 / MDM-2: company CRUD administration beyond onboarding. */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository repository;

    private CompanyService service;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CompanyService(repository);
    }

    @Test
    void updateChangesTheAmendableFieldsOnly() {
        Company company = Company.create(tenantId, "Acme (Pvt) Ltd", "REG-1", "VAT-1", "Old Address", "LKR", 1);
        UUID id = UUID.randomUUID();
        setId(company, id);
        when(repository.findById(id)).thenReturn(Optional.of(company));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Company updated = service.update(id, "Acme Holdings (Pvt) Ltd", "New Address", "https://cdn/logo.png");

        assertThat(updated.getLegalName()).isEqualTo("Acme Holdings (Pvt) Ltd");
        assertThat(updated.getAddress()).isEqualTo("New Address");
        assertThat(updated.getLogoUrl()).isEqualTo("https://cdn/logo.png");
        assertThat(updated.getRegistrationNo()).isEqualTo("REG-1");
        assertThat(updated.getBaseCurrency()).isEqualTo("LKR");
    }

    @Test
    void listForTenantReturnsEveryCompanyOfTheTenant() {
        Company a = Company.create(tenantId, "Company A", null, null, null, "LKR", 1);
        Company b = Company.create(tenantId, "Company B", null, null, null, "USD", 4);
        when(repository.findByTenantId(tenantId)).thenReturn(List.of(a, b));

        assertThat(service.listForTenant(tenantId)).containsExactly(a, b);
    }

    private static void setId(Company company, UUID id) {
        try {
            var idField = Company.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(company, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
