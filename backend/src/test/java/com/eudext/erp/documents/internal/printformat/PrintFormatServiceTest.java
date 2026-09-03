package com.eudext.erp.documents.internal.printformat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** DOC-2: template CRUD, one enabled default per company + document type. */
@ExtendWith(MockitoExtension.class)
class PrintFormatServiceTest {

    @Mock
    private PrintFormatRepository repository;

    private PrintFormatService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PrintFormatService(repository);
    }

    @Test
    void createsAPrintFormat() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrintFormat printFormat =
                service.create(tenantId, companyId, "sales:invoice", "Standard", "<html/>", false);

        assertThat(printFormat.getName()).isEqualTo("Standard");
        assertThat(printFormat.isDefault()).isFalse();
    }

    @Test
    void rejectsAMalformedDocumentType() {
        assertThatThrownBy(() -> service.create(tenantId, companyId, "SalesInvoice", "Standard", "<html/>", false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void settingANewDefaultClearsThePreviousOneFirst() {
        PrintFormat oldDefault = PrintFormat.create(tenantId, companyId, "sales:invoice", "Old", "<html/>");
        setId(oldDefault, UUID.randomUUID());
        markDefault(oldDefault);

        PrintFormat newFormat = PrintFormat.create(tenantId, companyId, "sales:invoice", "New", "<html/>");
        UUID newId = UUID.randomUUID();
        setId(newFormat, newId);

        when(repository.findById(newId)).thenReturn(Optional.of(newFormat));
        when(repository.findByCompanyIdAndDocumentTypeAndIsDefaultTrueAndDisabledFalse(companyId, "sales:invoice"))
                .thenReturn(Optional.of(oldDefault));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PrintFormat result = service.setDefault(newId);

        assertThat(oldDefault.isDefault()).isFalse();
        assertThat(result.isDefault()).isTrue();
    }

    @Test
    void disablingAFormatClearsItsDefaultFlag() {
        PrintFormat printFormat = PrintFormat.create(tenantId, companyId, "sales:invoice", "Standard", "<html/>");
        UUID id = UUID.randomUUID();
        setId(printFormat, id);
        markDefault(printFormat);
        when(repository.findById(id)).thenReturn(Optional.of(printFormat));

        service.disable(id);

        assertThat(printFormat.isDisabled()).isTrue();
        assertThat(printFormat.isDefault()).isFalse();
    }

    private static void markDefault(PrintFormat printFormat) {
        try {
            var method = PrintFormat.class.getDeclaredMethod("markDefault");
            method.setAccessible(true);
            method.invoke(printFormat);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void setId(PrintFormat printFormat, UUID id) {
        try {
            Field idField = PrintFormat.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(printFormat, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
