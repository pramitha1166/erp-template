package com.eudext.erp.numbering.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.numbering.internal.NumberingResetPolicy;
import com.eudext.erp.numbering.internal.NumberingSeries;
import com.eudext.erp.numbering.internal.SeriesConfigService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** NUM-1 / NUM-3: naming-series configuration — prefix/date-part template, counter width, and fiscal-year reset policy. */
@RestController
@RequestMapping("/numbering/series")
public class NumberingSeriesController {

    private static final String PERMISSION_MANAGE = "numbering:series:manage";
    private static final String PERMISSION_VIEW = "numbering:series:view";

    private final SeriesConfigService configService;
    private final NumberingAccessControl accessControl;

    public NumberingSeriesController(SeriesConfigService configService, NumberingAccessControl accessControl) {
        this.configService = configService;
        this.accessControl = accessControl;
    }

    public record ConfigureSeriesRequest(
            @NotBlank String docType,
            @NotBlank String prefix,
            @Min(1) @Max(10) int counterWidth,
            @NotNull NumberingResetPolicy resetPolicy,
            @Min(1) @Max(12) int fiscalYearStartMonth) {}

    public record SeriesView(
            UUID id,
            UUID companyId,
            String docType,
            String prefix,
            int counterWidth,
            NumberingResetPolicy resetPolicy,
            int fiscalYearStartMonth,
            boolean active,
            long nextCounter) {
        static SeriesView from(NumberingSeries series) {
            return new SeriesView(
                    series.getId(),
                    series.getCompanyId(),
                    series.getDocType(),
                    series.getPrefix(),
                    series.getCounterWidth(),
                    series.getResetPolicy(),
                    series.getFiscalYearStartMonth(),
                    series.isActive(),
                    series.getNextCounter());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public SeriesView configure(@RequestParam UUID companyId, @Valid @RequestBody ConfigureSeriesRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        NumberingSeries series = configService.configure(
                tenantId(),
                companyId,
                request.docType(),
                request.prefix(),
                request.counterWidth(),
                request.resetPolicy(),
                request.fiscalYearStartMonth());
        return SeriesView.from(series);
    }

    @GetMapping
    public List<SeriesView> listSeries(@RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return configService.seriesFor(companyId).stream().map(SeriesView::from).toList();
    }

    @PostMapping("/{seriesId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable UUID seriesId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        configService.activate(seriesId);
    }

    @PostMapping("/{seriesId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID seriesId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        configService.deactivate(seriesId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
