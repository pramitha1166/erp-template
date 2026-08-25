import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { VersionConflictDialog } from "./version-conflict-dialog";

describe("VersionConflictDialog", () => {
  it("renders nothing when closed", () => {
    render(
      <VersionConflictDialog open={false} onOpenChange={vi.fn()} onReload={vi.fn()} />,
    );

    expect(screen.queryByText("This document changed elsewhere")).not.toBeInTheDocument();
  });

  it("shows the conflict message when open", () => {
    render(
      <VersionConflictDialog open onOpenChange={vi.fn()} onReload={vi.fn()} />,
    );

    expect(screen.getByText("This document changed elsewhere")).toBeInTheDocument();
  });

  it("calls onReload when the reload action is clicked", async () => {
    const user = userEvent.setup();
    const onReload = vi.fn();
    render(<VersionConflictDialog open onOpenChange={vi.fn()} onReload={onReload} />);

    await user.click(screen.getByRole("button", { name: "Reload latest version" }));

    expect(onReload).toHaveBeenCalledOnce();
  });

  it("calls onOpenChange(false) when the operator chooses to keep editing", async () => {
    const user = userEvent.setup();
    const onOpenChange = vi.fn();
    render(<VersionConflictDialog open onOpenChange={onOpenChange} onReload={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "Keep editing" }));

    expect(onOpenChange).toHaveBeenCalledWith(false);
  });
});
