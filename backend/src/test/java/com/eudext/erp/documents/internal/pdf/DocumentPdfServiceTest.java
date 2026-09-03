package com.eudext.erp.documents.internal.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.eudext.erp.documents.internal.printformat.PrintFormat;
import com.eudext.erp.documents.internal.printformat.PrintFormatService;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** DOC-3: resolves the print format (default, or a specific one for previews) then renders it. */
@ExtendWith(MockitoExtension.class)
class DocumentPdfServiceTest {

    private static final String TEMPLATE = "<html><body><p>Invoice</p></body></html>";

    @Mock
    private PrintFormatService printFormatService;

    private DocumentPdfService service;
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DocumentPdfService(printFormatService, new PdfRenderer());
    }

    @Test
    void rendersTheDefaultPrintFormatForADocumentType() {
        PrintFormat printFormat = PrintFormat.create(UUID.randomUUID(), companyId, "sales:invoice", "Standard", TEMPLATE);
        when(printFormatService.getDefaultFor(companyId, "sales:invoice")).thenReturn(Optional.of(printFormat));

        byte[] pdf = service.renderDefault(companyId, "sales:invoice", Map.of());

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void failsWithANoSuchElementExceptionWhenNoDefaultIsConfigured() {
        when(printFormatService.getDefaultFor(companyId, "sales:invoice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.renderDefault(companyId, "sales:invoice", Map.of()))
                .isInstanceOf(NoSuchElementException.class);
    }
}
