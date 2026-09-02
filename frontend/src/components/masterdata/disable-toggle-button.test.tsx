import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";

import { DisableToggleButton } from "./disable-toggle-button";

describe("DisableToggleButton", () => {
  it("offers to disable an enabled record", async () => {
    const user = userEvent.setup();
    const onToggle = vi.fn();
    render(<DisableToggleButton disabled={false} pending={false} onToggle={onToggle} />);

    const button = screen.getByRole("button", { name: "Disable" });
    await user.click(button);

    expect(onToggle).toHaveBeenCalledTimes(1);
  });

  it("offers to enable a disabled record", () => {
    render(<DisableToggleButton disabled={true} pending={false} onToggle={vi.fn()} />);
    expect(screen.getByRole("button", { name: "Enable" })).toBeInTheDocument();
  });

  it("disables itself while a toggle is pending", () => {
    render(<DisableToggleButton disabled={false} pending={true} onToggle={vi.fn()} />);
    expect(screen.getByRole("button", { name: "Disable" })).toBeDisabled();
  });
});
