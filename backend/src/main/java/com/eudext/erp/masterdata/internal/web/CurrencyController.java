package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.currency.Currency;
import com.eudext.erp.masterdata.internal.currency.CurrencyService;
import com.eudext.erp.masterdata.internal.currency.ExchangeRate;
import com.eudext.erp.masterdata.internal.currency.ExchangeRateSource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * MDM-8: currency master and date-effective exchange rates. Currencies are shared across a tenant's companies, so
 * mutating operations anchor their permission check on an {@code authorizingCompanyId} the caller manages.
 */
@RestController
@RequestMapping("/masterdata/currencies")
public class CurrencyController {

    private static final String PERMISSION_MANAGE = "masterdata:currency:manage";

    private final CurrencyService currencyService;
    private final MasterDataAccessControl accessControl;

    public CurrencyController(CurrencyService currencyService, MasterDataAccessControl accessControl) {
        this.currencyService = currencyService;
        this.accessControl = accessControl;
    }

    public record NewCurrencyRequest(@NotBlank String code, @NotBlank String name, String symbol, @Min(0) int decimalPlaces) {}

    public record RecordRateRequest(
            @NotBlank String currencyCode,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rateDate,
            @NotNull @DecimalMin("0.000001") BigDecimal rateToBase) {}

    public record CurrencyView(UUID id, String code, String name, String symbol, int decimalPlaces, boolean disabled) {
        static CurrencyView from(Currency currency) {
            return new CurrencyView(
                    currency.getId(), currency.getCode(), currency.getName(), currency.getSymbol(), currency.getDecimalPlaces(),
                    currency.isDisabled());
        }
    }

    public record ExchangeRateView(UUID id, String currencyCode, LocalDate rateDate, BigDecimal rateToBase, ExchangeRateSource source) {
        static ExchangeRateView from(ExchangeRate rate) {
            return new ExchangeRateView(rate.getId(), rate.getCurrencyCode(), rate.getRateDate(), rate.getRateToBase(), rate.getSource());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CurrencyView create(@RequestParam UUID authorizingCompanyId, @Valid @RequestBody NewCurrencyRequest request) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        return CurrencyView.from(
                currencyService.create(tenantId(), request.code(), request.name(), request.symbol(), request.decimalPlaces()));
    }

    /** Every currency enabled for the caller's own tenant — RLS already scopes this, no further check needed. */
    @GetMapping
    public List<CurrencyView> list() {
        accessControl.currentUserId();
        return currencyService.listForTenant(tenantId()).stream().map(CurrencyView::from).toList();
    }

    @PostMapping("/{currencyId}/disable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID currencyId, @RequestParam UUID authorizingCompanyId) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        currencyService.disable(currencyId);
    }

    @PostMapping("/{currencyId}/enable")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enable(@PathVariable UUID currencyId, @RequestParam UUID authorizingCompanyId) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        currencyService.enable(currencyId);
    }

    @PostMapping("/rates")
    @ResponseStatus(HttpStatus.OK)
    public ExchangeRateView recordRate(@RequestParam UUID authorizingCompanyId, @Valid @RequestBody RecordRateRequest request) {
        accessControl.requirePermission(authorizingCompanyId, PERMISSION_MANAGE);
        ExchangeRate rate = currencyService.recordRate(
                tenantId(), request.currencyCode(), request.rateDate(), request.rateToBase(), ExchangeRateSource.MANUAL);
        return ExchangeRateView.from(rate);
    }

    @GetMapping("/{currencyCode}/rates")
    public List<ExchangeRateView> history(@PathVariable String currencyCode) {
        accessControl.currentUserId();
        return currencyService.history(tenantId(), currencyCode).stream().map(ExchangeRateView::from).toList();
    }

    @GetMapping("/{currencyCode}/rates/as-of")
    public ExchangeRateView rateAsOf(
            @PathVariable String currencyCode, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        accessControl.currentUserId();
        return ExchangeRateView.from(currencyService.rateAsOf(tenantId(), currencyCode, date));
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
