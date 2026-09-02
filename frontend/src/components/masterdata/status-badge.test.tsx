import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { StatusBadge } from "./status-badge";

describe("StatusBadge", () => {
  it("shows Active for an enabled record", () => {
    render(<StatusBadge disabled={false} />);
    expect(screen.getByText("Active")).toBeInTheDocument();
  });

  it("shows Disabled for a disabled record", () => {
    render(<StatusBadge disabled={true} />);
    expect(screen.getByText("Disabled")).toBeInTheDocument();
  });
});
