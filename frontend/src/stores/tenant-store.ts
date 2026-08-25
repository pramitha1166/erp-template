import { create } from "zustand";
import { persist } from "zustand/middleware";

/**
 * Active tenant/company context (ARCH-2, ORG-2). A user can hold different
 * roles per company (IAM-4), so this is switchable, not fixed at login.
 * Persisted so a reload doesn't drop the operator back to a company picker.
 */
export interface CompanyOption {
  id: string;
  name: string;
}

interface TenantState {
  tenantId: string | null;
  activeCompany: CompanyOption | null;
  availableCompanies: CompanyOption[];
  setAvailableCompanies: (companies: CompanyOption[]) => void;
  setActiveCompany: (company: CompanyOption) => void;
  setTenant: (tenantId: string) => void;
  clearTenantContext: () => void;
}

export const useTenantStore = create<TenantState>()(
  persist(
    (set) => ({
      tenantId: null,
      activeCompany: null,
      availableCompanies: [],
      setAvailableCompanies: (companies) =>
        set({ availableCompanies: companies }),
      setActiveCompany: (company) => set({ activeCompany: company }),
      setTenant: (tenantId) => set({ tenantId }),
      clearTenantContext: () =>
        set({ tenantId: null, activeCompany: null, availableCompanies: [] }),
    }),
    {
      name: "erp-tenant-context",
      partialize: (state) => ({
        tenantId: state.tenantId,
        activeCompany: state.activeCompany,
      }),
    },
  ),
);
