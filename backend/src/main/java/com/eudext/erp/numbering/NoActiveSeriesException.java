package com.eudext.erp.numbering;

import java.util.UUID;

/** NUM-1: raised when a caller asks for a number against a (companyId, docType) with no active series configured. */
public class NoActiveSeriesException extends RuntimeException {

    public NoActiveSeriesException(UUID companyId, String docType) {
        super("No active numbering series configured for company " + companyId + ", doc type " + docType);
    }
}
