package com.eudext.erp.masterdata.internal.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/** MDM-8: a date-effective rate from {@code currencyCode} to the company's base currency. */
@Entity
@Table(name = "exchange_rates")
@EntityListeners(AuditingEntityListener.class)
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "currency_code", nullable = false, updatable = false)
    private String currencyCode;

    @Column(name = "rate_date", nullable = false, updatable = false)
    private LocalDate rateDate;

    @Column(name = "rate_to_base", nullable = false)
    private BigDecimal rateToBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ExchangeRateSource source = ExchangeRateSource.MANUAL;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ExchangeRate() {}

    public static ExchangeRate of(
            UUID tenantId, String currencyCode, LocalDate rateDate, BigDecimal rateToBase, ExchangeRateSource source) {
        if (rateToBase.signum() <= 0) {
            throw new IllegalArgumentException("rateToBase must be positive");
        }
        ExchangeRate rate = new ExchangeRate();
        rate.tenantId = tenantId;
        rate.currencyCode = currencyCode;
        rate.rateDate = rateDate;
        rate.rateToBase = rateToBase;
        rate.source = source;
        return rate;
    }

    public UUID getId() {
        return id;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public BigDecimal getRateToBase() {
        return rateToBase;
    }

    public ExchangeRateSource getSource() {
        return source;
    }
}
