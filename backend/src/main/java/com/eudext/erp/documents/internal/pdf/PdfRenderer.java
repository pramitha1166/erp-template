package com.eudext.erp.documents.internal.pdf;

import com.lowagie.text.DocumentException;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * DOC-2/DOC-3: merges a print-format template (Thymeleaf XHTML) with a data model, then rasterises the result to
 * PDF server-side with Flying Saucer — entirely on the server, so output depends only on the template and the data
 * passed in, never on a client's browser/OS/installed fonts. (The generated PDF's container metadata — creation
 * timestamp, trailer id — is stamped by the underlying PDF library per the PDF spec and is not part of what DOC-3
 * means by "deterministic": the same template + data always renders the same visible content and layout.)
 *
 * <p>Templates must be well-formed XML (self-closing void elements, properly nested/closed tags) — both Thymeleaf
 * in {@link TemplateMode#XML} and Flying Saucer's underlying XML parser reject anything else.
 */
@Component
class PdfRenderer {

    private final TemplateEngine templateEngine;

    PdfRenderer() {
        StringTemplateResolver templateResolver = new StringTemplateResolver();
        // Thymeleaf 3's HTML mode is a lenient HTML5 parser and doesn't guarantee well-formed
        // output; XML mode enforces it, which is what Flying Saucer's XML parser requires below.
        templateResolver.setTemplateMode(TemplateMode.XML);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(templateResolver);
        this.templateEngine = engine;
    }

    byte[] render(String templateContent, Map<String, Object> model) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(model);
        String xhtml = templateEngine.process(templateContent, context);
        return toPdf(xhtml);
    }

    private byte[] toPdf(String xhtml) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(output);
            return output.toByteArray();
        } catch (DocumentException e) {
            throw new PdfRenderingException("Failed to render print format to PDF", e);
        }
    }
}
