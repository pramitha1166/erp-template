package com.eudext.erp.documents.internal.attachment;

/** DOC-4: the result of running {@link VirusScanner#scan}. {@code status} is never {@link ScanStatus#PENDING}. */
record ScanOutcome(ScanStatus status, String message) {

    static ScanOutcome clean() {
        return new ScanOutcome(ScanStatus.CLEAN, null);
    }

    static ScanOutcome infected(String message) {
        return new ScanOutcome(ScanStatus.INFECTED, message);
    }

    static ScanOutcome failed(String message) {
        return new ScanOutcome(ScanStatus.FAILED, message);
    }
}
