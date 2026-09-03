package com.eudext.erp.documents.internal.printformat;

import com.eudext.erp.documents.internal.DocumentType;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** DOC-2: CRUD for print-format templates, one enabled default per company + document type. */
@Service
public class PrintFormatService {

    private final PrintFormatRepository repository;

    public PrintFormatService(PrintFormatRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public PrintFormat create(
            UUID tenantId, UUID companyId, String documentType, String name, String templateContent, boolean makeDefault) {
        DocumentType.validate(documentType);
        PrintFormat printFormat = repository.save(PrintFormat.create(tenantId, companyId, documentType, name, templateContent));
        if (makeDefault) {
            printFormat = setDefault(printFormat.getId());
        }
        return printFormat;
    }

    @Transactional
    public PrintFormat rename(UUID id, String name) {
        PrintFormat printFormat = get(id);
        printFormat.rename(name);
        return repository.save(printFormat);
    }

    @Transactional
    public PrintFormat updateTemplate(UUID id, String templateContent) {
        PrintFormat printFormat = get(id);
        printFormat.updateTemplate(templateContent);
        return repository.save(printFormat);
    }

    /**
     * Makes {@code id} the default print format for its company + document type, clearing any previous default
     * first (and flushing that clear to the database) so the partial unique index never transiently sees two
     * defaults at once.
     */
    @Transactional
    public PrintFormat setDefault(UUID id) {
        PrintFormat printFormat = get(id);
        if (printFormat.isDisabled()) {
            throw new IllegalStateException("Cannot make a disabled print format the default");
        }
        repository.findByCompanyIdAndDocumentTypeAndIsDefaultTrueAndDisabledFalse(
                        printFormat.getCompanyId(), printFormat.getDocumentType())
                .filter(current -> !current.getId().equals(id))
                .ifPresent(current -> {
                    current.unmarkDefault();
                    repository.saveAndFlush(current);
                });
        printFormat.markDefault();
        return repository.save(printFormat);
    }

    @Transactional
    public void disable(UUID id) {
        get(id).disable();
    }

    @Transactional
    public void enable(UUID id) {
        get(id).enable();
    }

    @Transactional(readOnly = true)
    public PrintFormat get(UUID id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("No such print format"));
    }

    @Transactional(readOnly = true)
    public List<PrintFormat> listFor(UUID companyId, String documentType) {
        return repository.findByCompanyIdAndDocumentType(companyId, documentType);
    }

    @Transactional(readOnly = true)
    public Optional<PrintFormat> getDefaultFor(UUID companyId, String documentType) {
        return repository.findByCompanyIdAndDocumentTypeAndIsDefaultTrueAndDisabledFalse(companyId, documentType);
    }
}
