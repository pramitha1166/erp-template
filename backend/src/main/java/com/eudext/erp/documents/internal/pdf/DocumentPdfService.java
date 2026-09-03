package com.eudext.erp.documents.internal.pdf;

import com.eudext.erp.documents.internal.printformat.PrintFormat;
import com.eudext.erp.documents.internal.printformat.PrintFormatService;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DOC-3: renders a document type's print format to a PDF byte array given the caller-supplied data model. */
@Service
public class DocumentPdfService {

    private final PrintFormatService printFormatService;
    private final PdfRenderer pdfRenderer;

    DocumentPdfService(PrintFormatService printFormatService, PdfRenderer pdfRenderer) {
        this.printFormatService = printFormatService;
        this.pdfRenderer = pdfRenderer;
    }

    /** Renders using the enabled default print format configured for {@code companyId} + {@code documentType}. */
    @Transactional(readOnly = true)
    public byte[] renderDefault(UUID companyId, String documentType, Map<String, Object> model) {
        PrintFormat printFormat = printFormatService
                .getDefaultFor(companyId, documentType)
                .orElseThrow(() -> new NoSuchElementException("No default print format configured for " + documentType));
        return pdfRenderer.render(printFormat.getTemplateContent(), model);
    }

    /** Renders using a specific print format, e.g. for a preview while editing it. */
    @Transactional(readOnly = true)
    public byte[] render(UUID printFormatId, Map<String, Object> model) {
        return pdfRenderer.render(printFormatService.get(printFormatId).getTemplateContent(), model);
    }
}
