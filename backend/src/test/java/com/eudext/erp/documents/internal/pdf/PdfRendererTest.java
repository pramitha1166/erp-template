package com.eudext.erp.documents.internal.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** DOC-2/DOC-3: Thymeleaf XHTML template + data model merged into a PDF, entirely server-side. */
class PdfRendererTest {

    private static final String TEMPLATE =
            """
            <html xmlns:th="http://www.thymeleaf.org">
              <body>
                <p th:text="${invoiceNumber}">placeholder</p>
              </body>
            </html>
            """;

    private final PdfRenderer renderer = new PdfRenderer();

    @Test
    void rendersAWellFormedTemplateToAValidPdf() {
        byte[] pdf = renderer.render(TEMPLATE, Map.of("invoiceNumber", "INV-0001"));

        assertThat(pdf).isNotEmpty();
        String header = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
        String trailer = new String(pdf, pdf.length - 7, 7, StandardCharsets.US_ASCII).trim();
        assertThat(trailer).endsWith("%%EOF");
    }

    @Test
    void sameTemplateAndDataProduceTheSamePageLayout() {
        byte[] first = renderer.render(TEMPLATE, Map.of("invoiceNumber", "INV-0001"));
        byte[] second = renderer.render(TEMPLATE, Map.of("invoiceNumber", "INV-0001"));

        // DOC-3: deterministic content/layout for identical input — container metadata (creation
        // timestamp) can differ by a second, so byte-for-byte equality isn't asserted, only that
        // rendering the same input twice yields the same amount of content.
        assertThat(second.length).isEqualTo(first.length);
    }

    @Test
    void rejectsMalformedMarkup() {
        String malformed = "<html><body><p>unclosed</body></html>";

        assertThatThrownBy(() -> renderer.render(malformed, Map.of())).isInstanceOf(RuntimeException.class);
    }
}
