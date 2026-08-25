import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it } from "vitest";

import { useTenantStore } from "@/stores/tenant-store";
import { TenantIndicator } from "./tenant-indicator";

afterEach(() => {
  useTenantStore.getState().clearTenantContext();
});

describe("TenantIndicator", () => {
  it("shows a fallback when no company is active", () => {
    render(<TenantIndicator />);

    expect(screen.getByText("No company selected")).toBeInTheDocument();
  });

  it("shows the active company's name once one is set", () => {
    useTenantStore.getState().setActiveCompany({ id: "co-1", name: "Acme Lanka" });

    render(<TenantIndicator />);

    expect(screen.getByText("Acme Lanka")).toBeInTheDocument();
  });
});
