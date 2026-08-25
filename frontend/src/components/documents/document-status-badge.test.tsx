import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { DocumentStatusBadge } from "./document-status-badge";

describe("DocumentStatusBadge", () => {
  it.each([
    ["DRAFT", "Draft"],
    ["SUBMITTED", "Submitted"],
    ["CANCELLED", "Cancelled"],
    ["AMENDED", "Amended"],
  ] as const)("renders the %s label", (status, label) => {
    render(<DocumentStatusBadge status={status} />);

    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
