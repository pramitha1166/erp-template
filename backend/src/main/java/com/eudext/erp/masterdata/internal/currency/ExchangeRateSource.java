package com.eudext.erp.masterdata.internal.currency;

/** MDM-8: whether an {@link ExchangeRate} was keyed in by hand or pulled by the optional CBSL import job. */
public enum ExchangeRateSource {
    MANUAL,
    CBSL
}
