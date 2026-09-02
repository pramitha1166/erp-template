import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { AppShell } from "./app-shell";

// The header's session menu (F0.2.1) reads `next/navigation`'s router for
// sign-out — outside a real Next.js app router tree that throws, so stub it.
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn(), forward: vi.fn(), refresh: vi.fn(), prefetch: vi.fn() }),
}));

// F0.6.1: the company switcher (in the header) fetches the tenant's company list to populate itself.
vi.mock("@/lib/api/masterdata-company-api", () => ({
  listCompanies: vi.fn().mockResolvedValue([]),
}));

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(<QueryClientProvider client={queryClient}>{ui}</QueryClientProvider>);
}

describe("AppShell", () => {
  it("renders the header and the primary nav", () => {
    renderWithClient(
      <AppShell>
        <p>page content</p>
      </AppShell>,
    );

    expect(screen.getByText("ERP Platform")).toBeInTheDocument();
    expect(screen.getByRole("navigation", { name: "Primary" })).toBeInTheDocument();
    expect(screen.getByText("page content")).toBeInTheDocument();
  });

  it("toggles the mobile nav open state when the menu button is clicked", async () => {
    const user = userEvent.setup();
    renderWithClient(
      <AppShell>
        <p>page content</p>
      </AppShell>,
    );

    const nav = screen.getByRole("navigation", { name: "Primary" });
    const classTokens = () => nav.className.split(" ");
    expect(classTokens()).not.toContain("translate-x-0");

    await user.click(screen.getByRole("button", { name: "Toggle navigation" }));

    expect(classTokens()).toContain("translate-x-0");
  });
});
