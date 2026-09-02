"use client";

import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import { Form } from "@/components/ui/form";
import { ValidatedSelectField } from "@/components/form/validated-select-field";
import { ValidatedTextField } from "@/components/form/validated-text-field";
import { FormControl, FormField, FormItem, FormLabel } from "@/components/ui/form";
import { ApiError } from "@/lib/api/http";
import {
  ACCOUNT_TYPE_OPTIONS,
  type AccountView,
  activateAccount,
  createAccount,
  deactivateAccount,
  listAccounts,
  renameAccount,
} from "@/lib/api/masterdata-coa-api";
import { flattenTree } from "@/lib/masterdata/tree";
import { DisableToggleButton } from "./disable-toggle-button";
import { StatusBadge } from "./status-badge";

const formSchema = z.object({
  code: z.string().min(1, "Code is required"),
  name: z.string().min(1, "Name is required"),
  accountType: z.enum(["ASSET", "LIABILITY", "EQUITY", "INCOME", "EXPENSE"]),
  parentId: z.string(),
  group: z.boolean(),
});
type FormValues = z.infer<typeof formSchema>;

const emptyFormValues: FormValues = { code: "", name: "", accountType: "ASSET", parentId: "", group: true };

export interface ChartOfAccountsPanelProps {
  companyId: string;
}

/**
 * F0.6.2 / MDM-3: Chart of Accounts tree — create/rename nodes, the group-vs-ledger toggle, and activate/deactivate.
 * A child account's type is locked to its parent's (`AccountService.create` rejects a mismatch), and only group
 * accounts are offered as a parent (a ledger account can't have children).
 */
export function ChartOfAccountsPanel({ companyId }: ChartOfAccountsPanelProps) {
  const queryClient = useQueryClient();
  const queryKey = ["masterdata", "accounts", companyId];
  const [editingAccount, setEditingAccount] = useState<AccountView | null>(null);

  const {
    data: accounts,
    isLoading,
    isError,
  } = useQuery({ queryKey, queryFn: () => listAccounts(companyId) });

  const rows = accounts ? flattenTree(accounts.map((a) => ({ ...a, parentId: a.parentId }))) : [];

  const form = useForm<FormValues>({ resolver: zodResolver(formSchema), defaultValues: emptyFormValues });
  const selectedParentId = form.watch("parentId");

  useEffect(() => {
    if (!selectedParentId || !accounts) {
      return;
    }
    const parent = accounts.find((a) => a.id === selectedParentId);
    if (parent) {
      form.setValue("accountType", parent.accountType);
    }
  }, [selectedParentId, accounts, form]);

  const saveMutation = useMutation({
    mutationFn: (values: FormValues) =>
      editingAccount
        ? renameAccount(editingAccount.id, companyId, values.name)
        : createAccount(companyId, {
            code: values.code,
            name: values.name,
            accountType: values.accountType,
            parentId: values.parentId || null,
            group: values.group,
          }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      form.reset(emptyFormValues);
      setEditingAccount(null);
    },
  });

  const toggleMutation = useMutation({
    mutationFn: (target: AccountView) =>
      target.active ? deactivateAccount(target.id, companyId) : activateAccount(target.id, companyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey }),
  });

  function startEditing(account: AccountView) {
    setEditingAccount(account);
    form.reset({
      code: account.code,
      name: account.name,
      accountType: account.accountType,
      parentId: account.parentId ?? "",
      group: account.group,
    });
  }

  function cancelEditing() {
    setEditingAccount(null);
    form.reset(emptyFormValues);
  }

  const parentOptions = [
    { value: "", label: "(no parent — top level)" },
    ...(accounts ?? []).filter((a) => a.group).map((a) => ({ value: a.id, label: `${a.code} — ${a.name}` })),
  ];

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-3 rounded-lg border p-4">
        <h2 className="text-sm font-semibold">Chart of Accounts</h2>

        {isLoading && <p className="text-sm text-muted-foreground">Loading accounts…</p>}
        {isError && (
          <p role="alert" className="text-sm text-destructive">
            Could not load the chart of accounts.
          </p>
        )}
        {accounts && accounts.length === 0 && <p className="text-sm text-muted-foreground">No accounts yet.</p>}
        {rows.length > 0 && (
          <ul className="flex flex-col gap-2">
            {rows.map(({ item: account, depth }) => (
              <li
                key={account.id}
                style={{ marginLeft: depth * 20 }}
                className="flex flex-wrap items-center justify-between gap-2 rounded-md border px-3 py-2 text-sm"
              >
                <div className="flex flex-col">
                  <span className="font-medium">
                    {account.code} — {account.name}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {account.accountType} · {account.group ? "Group" : "Ledger"}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge disabled={!account.active} />
                  <Button size="sm" variant="ghost" onClick={() => startEditing(account)}>
                    Rename
                  </Button>
                  <DisableToggleButton
                    disabled={!account.active}
                    pending={toggleMutation.isPending}
                    onToggle={() => toggleMutation.mutate(account)}
                  />
                </div>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="flex flex-col gap-3 rounded-lg border border-dashed p-4">
        <h3 className="text-sm font-semibold">{editingAccount ? `Rename ${editingAccount.code}` : "Add an account"}</h3>
        <Form {...form}>
          <form onSubmit={form.handleSubmit((values) => saveMutation.mutate(values))} className="flex flex-col gap-3">
            {editingAccount ? (
              <p className="text-sm text-muted-foreground">
                Code: <span className="font-medium text-foreground">{editingAccount.code}</span> ({editingAccount.accountType},{" "}
                {editingAccount.group ? "group" : "ledger"})
              </p>
            ) : (
              <>
                <ValidatedTextField control={form.control} name="code" label="Code" />
                <ValidatedSelectField control={form.control} name="parentId" label="Parent" options={parentOptions} />
                <ValidatedSelectField
                  control={form.control}
                  name="accountType"
                  label="Account type"
                  options={ACCOUNT_TYPE_OPTIONS}
                />
                {selectedParentId && (
                  <p className="text-xs text-muted-foreground">Account type is fixed to the parent&apos;s type.</p>
                )}
                <FormField
                  control={form.control}
                  name="group"
                  render={({ field }) => (
                    <FormItem className="flex flex-row items-center gap-2 space-y-0">
                      <FormControl>
                        <input
                          type="checkbox"
                          className="size-4"
                          checked={field.value}
                          onChange={(e) => field.onChange(e.target.checked)}
                        />
                      </FormControl>
                      <FormLabel className="!mt-0">Group account (can have children)</FormLabel>
                    </FormItem>
                  )}
                />
              </>
            )}
            <ValidatedTextField control={form.control} name="name" label="Name" />

            {saveMutation.isError && (
              <p role="alert" className="text-sm text-destructive">
                {saveMutation.error instanceof ApiError ? saveMutation.error.message : "Could not save this account."}
              </p>
            )}

            <div className="flex gap-2">
              <Button type="submit" disabled={saveMutation.isPending} className="self-start">
                {saveMutation.isPending ? "Saving…" : editingAccount ? "Save changes" : "Add account"}
              </Button>
              {editingAccount && (
                <Button type="button" variant="ghost" onClick={cancelEditing}>
                  Cancel
                </Button>
              )}
            </div>
          </form>
        </Form>
      </section>
    </div>
  );
}
