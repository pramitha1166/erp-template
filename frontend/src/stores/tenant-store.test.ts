import { afterEach, describe, expect, it } from "vitest";

import { useTenantStore } from "./tenant-store";

afterEach(() => {
  useTenantStore.getState().clearTenantContext();
});

describe("useTenantStore", () => {
  it("starts with no active company", () => {
    expect(useTenantStore.getState().activeCompany).toBeNull();
  });

  it("sets the active company", () => {
    const company = { id: "co-1", name: "Acme Lanka" };
    useTenantStore.getState().setActiveCompany(company);

    expect(useTenantStore.getState().activeCompany).toEqual(company);
  });

  it("clears tenant and company context together", () => {
    useTenantStore.getState().setTenant("tenant-1");
    useTenantStore.getState().setActiveCompany({ id: "co-1", name: "Acme Lanka" });

    useTenantStore.getState().clearTenantContext();

    expect(useTenantStore.getState().tenantId).toBeNull();
    expect(useTenantStore.getState().activeCompany).toBeNull();
  });
});
