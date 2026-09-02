package com.eudext.erp.masterdata.internal.web;

import com.eudext.erp.config.tenancy.TenantContext;
import com.eudext.erp.masterdata.internal.coa.Account;
import com.eudext.erp.masterdata.internal.coa.AccountService;
import com.eudext.erp.masterdata.internal.coa.AccountType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** MDM-3: Chart of Accounts administration — hierarchical, group-vs-ledger nodes beyond the onboarding seed. */
@RestController
@RequestMapping("/masterdata/accounts")
public class AccountController {

    private static final String PERMISSION_MANAGE = "masterdata:coa:manage";
    private static final String PERMISSION_VIEW = "masterdata:coa:view";

    private final AccountService accountService;
    private final MasterDataAccessControl accessControl;

    public AccountController(AccountService accountService, MasterDataAccessControl accessControl) {
        this.accountService = accountService;
        this.accessControl = accessControl;
    }

    public record NewAccountRequest(
            @NotBlank String code, @NotBlank String name, @NotNull AccountType accountType, UUID parentId, boolean group) {}

    public record RenameAccountRequest(@NotBlank String name) {}

    public record AccountView(
            UUID id, String code, String name, AccountType accountType, UUID parentId, boolean group, boolean active) {
        static AccountView from(Account account) {
            return new AccountView(
                    account.getId(),
                    account.getCode(),
                    account.getName(),
                    account.getAccountType(),
                    account.getParentId(),
                    account.isGroup(),
                    account.isActive());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView create(@RequestParam UUID companyId, @Valid @RequestBody NewAccountRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        Account account = accountService.create(
                tenantId(), companyId, request.code(), request.name(), request.accountType(), request.parentId(), request.group());
        return AccountView.from(account);
    }

    @GetMapping
    public List<AccountView> list(@RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_VIEW);
        return accountService.listForCompany(companyId).stream().map(AccountView::from).toList();
    }

    @PutMapping("/{accountId}")
    public AccountView rename(
            @PathVariable UUID accountId, @RequestParam UUID companyId, @Valid @RequestBody RenameAccountRequest request) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        return AccountView.from(accountService.rename(accountId, request.name()));
    }

    @PostMapping("/{accountId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID accountId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        accountService.deactivate(accountId);
    }

    @PostMapping("/{accountId}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable UUID accountId, @RequestParam UUID companyId) {
        accessControl.requirePermission(companyId, PERMISSION_MANAGE);
        accountService.activate(accountId);
    }

    private UUID tenantId() {
        return TenantContext.get().orElseThrow(() -> new IllegalStateException("No tenant context"));
    }
}
