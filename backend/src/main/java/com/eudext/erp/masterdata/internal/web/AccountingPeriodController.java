package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.masterdata.internal.fiscalyear.AccountingPeriod;
import com.eudext.erp.masterdata.internal.fiscalyear.AccountingPeriodService;
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

/** MDM-9: accounting period open/close administration. */
@RestController
@RequestMapping("/masterdata/accounting-periods")
public class AccountingPeriodController {

    private static final String PERMISSION_MANAGE = "masterdata:fiscalyear:manage";
    private static final String PERMISSION_VIEW = "masterdata:fiscalyear:view";

    private final AccountingPeriodService periodService;
    private final MasterDataAccessControl accessControl;

    public AccountingPeriodController(AccountingPeriodService periodService, MasterDataAccessControl accessControl) {
        this.periodService = periodService;
        this.accessControl = accessControl;
    }

    public record AccountingPeriodView(UUID id, UUID fiscalYearId, String name, LocalDate startDate, LocalDate endDate,
            FiscalYearStatus status) {
        static AccountingPeriodView from(AccountingPeriod period) {
            return new AccountingPeriodView(
                    period.getId(), period.getFiscalYearId(), period.getName(), period.getStartDate(), period.getEndDate(),
                    period.getStatus());
        }
    }

    @GetMapping
    public List<AccountingPeriodView> list(@RequestParam UUID fiscalYearId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return periodService.listForFiscalYear(fiscalYearId).stream().map(AccountingPeriodView::from).toList();
    }

    @PostMapping("/{periodId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable UUID periodId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        periodService.close(periodId);
    }

    @PostMapping("/{periodId}/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(@PathVariable UUID periodId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        periodService.reopen(periodId);
    }
}
