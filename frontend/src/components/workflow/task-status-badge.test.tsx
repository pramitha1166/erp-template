import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { TaskStatusBadge } from "./task-status-badge";

describe("TaskStatusBadge", () => {
  it.each([
    ["PENDING", "Pending"],
    ["APPROVED", "Approved"],
    ["REJECTED", "Rejected"],
    ["CANCELLED", "Cancelled"],
    ["ESCALATED", "Escalated"],
  ] as const)("renders the %s label", (status, label) => {
    render(<TaskStatusBadge status={status} />);

    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
