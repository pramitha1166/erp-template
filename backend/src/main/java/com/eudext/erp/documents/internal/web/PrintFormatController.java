package com.eudext.erp.documents.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.documents.internal.pdf.DocumentPdfService;
import com.eudext.erp.documents.internal.printformat.PrintFormat;
import com.eudext.erp.documents.internal.printformat.PrintFormatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** DOC-2/DOC-3/DOC-5: configurable print-format templates per document type, and PDF preview rendering. */
@RestController
@RequestMapping("/documents/print-formats")
public class PrintFormatController {

    private final PrintFormatService printFormatService;
    private final DocumentPdfService documentPdfService;
    private final DocumentsAccessControl accessControl;

    public PrintFormatController(
            PrintFormatService printFormatService, DocumentPdfService documentPdfService, DocumentsAccessControl accessControl) {
        this.printFormatService = printFormatService;
        this.documentPdfService = documentPdfService;
        this.accessControl = accessControl;
    }

    public record NewPrintFormatRequest(
            @NotBlank String documentType, @NotBlank String name, @NotBlank String templateContent, boolean makeDefault) {}

    public record RenamePrintFormatRequest(@NotBlank String name) {}

    public record UpdateTemplateRequest(@NotBlank String templateContent) {}

    public record PrintFormatView(
            UUID id, String documentType, String name, boolean isDefault, String templateContent, boolean disabled) {
        static PrintFormatView from(PrintFormat printFormat) {
            return new PrintFormatView(
                    printFormat.getId(),
                    printFormat.getDocumentType(),
                    printFormat.getName(),
                    printFormat.isDefault(),
                    printFormat.getTemplateContent(),
                    printFormat.isDisabled());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PrintFormatView create(@RequestParam UUID companyId, @Valid @RequestBody NewPrintFormatRequest request) {
        accessControl.requireDocumentPermission(companyId, request.documentType(), "manage");
        return PrintFormatView.from(printFormatService.create(
                tenantId(), companyId, request.documentType(), request.name(), request.templateContent(), request.makeDefault()));
    }

    @GetMapping
    public List<PrintFormatView> list(@RequestParam UUID companyId, @RequestParam String documentType) {
        accessControl.requireDocumentPermission(companyId, documentType, "view");
        return printFormatService.listFor(companyId, documentType).stream()
                .map(PrintFormatView::from)
                .toList();
    }

    @PutMapping("/{printFormatId}")
    public PrintFormatView rename(@PathVariable UUID printFormatId, @Valid @RequestBody RenamePrintFormatRequest request) {
        PrintFormat printFormat = requirePermission(printFormatId, "manage");
        return PrintFormatView.from(printFormatService.rename(printFormat.getId(), request.name()));
    }

    @PutMapping("/{printFormatId}/template")
    public PrintFormatView updateTemplate(@PathVariable UUID printFormatId, @Valid @RequestBody UpdateTemplateRequest request) {
        PrintFormat printFormat = requirePermission(printFormatId, "manage");
        return PrintFormatView.from(printFormatService.updateTemplate(printFormat.getId(), request.templateContent()));
    }

    @PostMapping("/{printFormatId}/default")
    public PrintFormatView setDefault(@PathVariable UUID printFormatId) {
        PrintFormat printFormat = requirePermission(printFormatId, "manage");
        return PrintFormatView.from(printFormatService.setDefault(printFormat.getId()));
    }

    @PostMapping("/{printFormatId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID printFormatId) {
        PrintFormat printFormat = requirePermission(printFormatId, "manage");
        printFormatService.disable(printFormat.getId());
    }

    @PostMapping("/{printFormatId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID printFormatId) {
        PrintFormat printFormat = requirePermission(printFormatId, "manage");
        printFormatService.enable(printFormat.getId());
    }

    /** DOC-3: renders a specific print format against a caller-supplied data model — used for template previews. */
    @PostMapping(value = "/{printFormatId}/render", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> render(@PathVariable UUID printFormatId, @RequestBody Map<String, Object> model) {
        PrintFormat printFormat = requirePermission(printFormatId, "view");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(documentPdfService.render(printFormat.getId(), model));
    }

    /** DOC-3: renders using the enabled default print format for {@code companyId} + {@code documentType}. */
    @PostMapping(value = "/render-default", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> renderDefault(
            @RequestParam UUID companyId, @RequestParam String documentType, @RequestBody Map<String, Object> model) {
        accessControl.requireDocumentPermission(companyId, documentType, "view");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(documentPdfService.renderDefault(companyId, documentType, model));
    }

    private PrintFormat requirePermission(UUID printFormatId, String action) {
        PrintFormat printFormat = printFormatService.get(printFormatId);
        accessControl.requireDocumentPermission(printFormat.getCompanyId(), printFormat.getDocumentType(), action);
        return printFormat;
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
