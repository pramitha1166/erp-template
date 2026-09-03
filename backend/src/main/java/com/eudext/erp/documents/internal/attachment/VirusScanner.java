package com.eudext.erp.documents.internal.attachment;

/**
 * DOC-4: scans uploaded content for malware before it is stored. Kept as an interface — separate from the ClamAV
 * client — so {@link AttachmentService} is unit-testable without a reachable ClamAV daemon. {@link
 * ClamAvVirusScanner} is the only production implementation, backed by the ClamAV instance docker-compose
 * provisions locally (NFR-D1) and a reachable ClamAV endpoint in staging/production.
 */
interface VirusScanner {

    ScanOutcome scan(byte[] content);
}
