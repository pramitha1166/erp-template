package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYear;
import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYearService;
import com.eudext.erp.masterdata.internal.fiscalyear.FiscalYearStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** MDM-9: fiscal year administration — close/reopen beyond the onboarding-seeded default year. */
@RestController
@RequestMapping("/masterdata/fiscal-years")
public class FiscalYearController {

    private static final String PERMISSION_MANAGE = "masterdata:fiscalyear:manage";
    private static final String PERMISSION_VIEW = "masterdata:fiscalyear:view";

    private final FiscalYearService fiscalYearService;
    private final MasterDataAccessControl accessControl;

    public FiscalYearController(FiscalYearService fiscalYearService, MasterDataAccessControl accessControl) {
        this.fiscalYearService = fiscalYearService;
        this.accessControl = accessControl;
    }

    public record FiscalYearView(UUID id, String name, LocalDate startDate, LocalDate endDate, FiscalYearStatus status) {
        static FiscalYearView from(FiscalYear fiscalYear) {
            return new FiscalYearView(
                    fiscalYear.getId(), fiscalYear.getName(), fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                    fiscalYear.getStatus());
        }
    }

    @GetMapping
    public List<FiscalYearView> list(@RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return fiscalYearService.listForCompany(companyId).stream().map(FiscalYearView::from).toList();
    }

    @GetMapping("/{fiscalYearId}")
    public FiscalYearView get(@PathVariable UUID fiscalYearId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return FiscalYearView.from(fiscalYearService.get(fiscalYearId));
    }

    @PostMapping("/{fiscalYearId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable UUID fiscalYearId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        fiscalYearService.close(fiscalYearId);
    }

    @PostMapping("/{fiscalYearId}/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(@PathVariable UUID fiscalYearId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        fiscalYearService.reopen(fiscalYearId);
    }
}
