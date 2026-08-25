import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it } from "vitest";

import { AppShell } from "./app-shell";

describe("AppShell", () => {
  it("renders the header and the primary nav", () => {
    render(
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
    render(
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
