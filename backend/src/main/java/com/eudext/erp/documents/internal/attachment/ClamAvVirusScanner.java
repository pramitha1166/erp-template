package com.eudext.erp.documents.internal.attachment;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.ClamavException;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

/** DOC-4: scans upload content against a ClamAV daemon over its INSTREAM protocol. */
class ClamAvVirusScanner implements VirusScanner {

    private final ClamavClient clamavClient;

    ClamAvVirusScanner(ClamavClient clamavClient) {
        this.clamavClient = clamavClient;
    }

    @Override
    public ScanOutcome scan(byte[] content) {
        try {
            ScanResult result = clamavClient.scan(new ByteArrayInputStream(content));
            if (result instanceof ScanResult.OK) {
                return ScanOutcome.clean();
            }
            if (result instanceof ScanResult.VirusFound virusFound) {
                String viruses = virusFound.getFoundViruses().values().stream()
                        .flatMap(java.util.Collection::stream)
                        .collect(Collectors.joining(", "));
                return ScanOutcome.infected(viruses.isBlank() ? "Virus found" : viruses);
            }
            return ScanOutcome.failed("Unrecognised ClamAV scan result: " + result);
        } catch (ClamavException e) {
            return ScanOutcome.failed("ClamAV scan failed: " + e.getMessage());
        }
    }
}
