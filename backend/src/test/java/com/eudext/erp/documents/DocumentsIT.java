package com.eudext.erp.documents;

import static org.assertj.core.api.Assertions.assertThat;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.documents.internal.attachment.Attachment;
import com.eudext.erp.documents.internal.attachment.AttachmentRepository;
import com.eudext.erp.documents.internal.attachment.ScanStatus;
import com.eudext.erp.documents.internal.pdf.DocumentPdfService;
import com.eudext.erp.documents.internal.printformat.PrintFormat;
import com.eudext.erp.documents.internal.printformat.PrintFormatService;
import com.eudext.erp.testsupport.AbstractIntegrationTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Epic 0.7 (PLAT-DOC) end to end against a real Postgres: attachment metadata (DOC-1) and print-format templates
 * (DOC-2) both live under RLS (ARCH-2), and a print format renders to a real PDF (DOC-3). Attachment upload/download
 * itself needs S3-compatible storage, which is off (by design, see {@code AttachmentStorageProperties}) outside the
 * {@code docker} profile, so it isn't exercised here — {@link com.eudext.erp.documents.internal.attachment.AttachmentServiceTest}
 * covers that with a fake {@code AttachmentStorage}.
 */
class DocumentsIT extends AbstractIntegrationTest {

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private PrintFormatService printFormatService;

    @Autowired
    private DocumentPdfService documentPdfService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(tenantId);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void attachmentMetadataIsScopedToItsOwnTenantByRls() {
        UUID documentId = UUID.randomUUID();
        Attachment attachment = attachmentRepository.save(Attachment.create(
                tenantId,
                companyId,
                "sales:invoice",
                documentId,
                "invoice.pdf",
                "application/pdf",
                5,
                "some/key/" + UUID.randomUUID(),
                "checksum",
                ScanStatus.CLEAN,
                null));

        assertThat(attachmentRepository.findByDocumentTypeAndDocumentId("sales:invoice", documentId))
                .extracting(Attachment::getId)
                .contains(attachment.getId());

        TenantContext.set(UUID.randomUUID());
        assertThat(attachmentRepository.findByDocumentTypeAndDocumentId("sales:invoice", documentId)).isEmpty();
    }

    @Test
    void printFormatDefaultToggleAndPdfRenderingWorkEndToEnd() {
        PrintFormat first = printFormatService.create(
                tenantId,
                companyId,
                "sales:invoice",
                "Standard",
                "<html xmlns:th=\"http://www.thymeleaf.org\"><body><p th:text=\"${invoiceNumber}\">x</p></body></html>",
                true);
        assertThat(printFormatService.getDefaultFor(companyId, "sales:invoice")).contains(first);

        PrintFormat second =
                printFormatService.create(tenantId, companyId, "sales:invoice", "Alternate", "<html/>", true);

        assertThat(printFormatService.getDefaultFor(companyId, "sales:invoice")).contains(second);
        assertThat(printFormatService.get(first.getId()).isDefault()).isFalse();

        byte[] pdf = documentPdfService.renderDefault(companyId, "sales:invoice", Map.of("invoiceNumber", "INV-0042"));
        assertThat(pdf).isNotEmpty();

        TenantContext.set(UUID.randomUUID());
        assertThat(printFormatService.listFor(companyId, "sales:invoice")).isEmpty();
    }
}
