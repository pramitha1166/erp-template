import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { AttachmentScanStatusBadge } from "./attachment-scan-status-badge";

describe("AttachmentScanStatusBadge", () => {
  it.each([
    ["PENDING", "Scan pending"],
    ["CLEAN", "Clean"],
    ["INFECTED", "Infected"],
    ["FAILED", "Scan failed"],
  ] as const)("renders the %s label", (status, label) => {
    render(<AttachmentScanStatusBadge status={status} />);

    expect(screen.getByText(label)).toBeInTheDocument();
  });
});
