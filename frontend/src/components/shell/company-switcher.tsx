"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Building2, Check } from "lucide-react";

import { cn } from "@/lib/utils";
import { listUserRoles } from "@/lib/api/iam-api";
import { useSessionStore } from "@/stores/session-store";
import { type CompanyOption, useTenantStore } from "@/stores/tenant-store";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

interface CompanyRowProps {
  company: CompanyOption;
  userId: string;
  isActive: boolean;
  onSelect: () => void;
}

function CompanyRow({ company, userId, isActive, onSelect }: CompanyRowProps) {
  const { data: roles } = useQuery({
    queryKey: ["user-roles", userId, company.id],
    queryFn: () => listUserRoles(userId, company.id),
    enabled: userId.length > 0,
    staleTime: 30_000,
  });

  return (
    <button
      type="button"
      onClick={onSelect}
      className={cn(
        "flex w-full items-center justify-between rounded-md border px-3 py-2 text-left text-sm transition-colors hover:bg-accent",
        isActive && "border-primary",
      )}
    >
      <span className="flex flex-col">
        <span className="font-medium">{company.name}</span>
        <span className="text-xs text-muted-foreground">
          {roles ? `${roles.length} role${roles.length === 1 ? "" : "s"} held here` : "Loading roles…"}
        </span>
      </span>
      {isActive && <Check className="size-4 shrink-0 text-primary" aria-hidden="true" />}
    </button>
  );
}

/**
 * F0.2.6 / IAM-4: a user can hold different roles in different companies of
 * the same tenant, so the active company is switchable rather than fixed at
 * login (see `tenant-store.ts`). `availableCompanies` has no backing
 * endpoint yet — company/branch masters are Epic 0.9+ (the same gap
 * `UserController` documents for user provisioning) — so this switches
 * among whatever the eventual company-picker populates the store with, and
 * says so plainly when that list is still empty.
 */
export function CompanySwitcher() {
  const [open, setOpen] = useState(false);
  const availableCompanies = useTenantStore((state) => state.availableCompanies);
  const activeCompany = useTenantStore((state) => state.activeCompany);
  const setActiveCompany = useTenantStore((state) => state.setActiveCompany);
  const userId = useSessionStore((state) => state.user?.id ?? "");

  return (
    <>
      <Button
        variant="ghost"
        size="sm"
        className="gap-1.5 text-muted-foreground"
        onClick={() => setOpen(true)}
      >
        <Building2 className="size-4" aria-hidden="true" />
        {activeCompany ? activeCompany.name : "No company selected"}
      </Button>
      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Switch company</DialogTitle>
            <DialogDescription>
              Switching updates which company&apos;s data and roles apply for the rest of this session.
            </DialogDescription>
          </DialogHeader>
          {availableCompanies.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              No companies available yet.
            </p>
          ) : (
            <div className="flex flex-col gap-2">
              {availableCompanies.map((company) => (
                <CompanyRow
                  key={company.id}
                  company={company}
                  userId={userId}
                  isActive={activeCompany?.id === company.id}
                  onSelect={() => {
                    setActiveCompany(company);
                    setOpen(false);
                  }}
                />
              ))}
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  );
}
