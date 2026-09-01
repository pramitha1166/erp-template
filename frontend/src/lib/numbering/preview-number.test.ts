import { describe, expect, it } from "vitest";

import { previewNextNumber, resolvePrefixPreview } from "./preview-number";

describe("resolvePrefixPreview", () => {
  it("resolves calendar-year date placeholders", () => {
    const date = new Date(2026, 8, 5); // September is month index 8
    expect(resolvePrefixPreview("INV-{YYYY}-{MM}-", date, 1)).toBe("INV-2026-09-");
    expect(resolvePrefixPreview("INV-{YY}-", date, 1)).toBe("INV-26-");
    expect(resolvePrefixPreview("SINV-", date, 1)).toBe("SINV-");
  });

  it("resolves the fiscal-year placeholder as the calendar year when the fiscal year starts in January", () => {
    expect(resolvePrefixPreview("INV-{FY}-", new Date(2026, 5, 1), 1)).toBe("INV-2026-");
  });

  it("resolves the fiscal-year placeholder as a range label for a non-calendar fiscal year", () => {
    expect(resolvePrefixPreview("INV-{FY}-", new Date(2026, 2, 1), 4)).toBe("INV-2025-26-");
    expect(resolvePrefixPreview("INV-{FY}-", new Date(2026, 3, 1), 4)).toBe("INV-2026-27-");
  });
});

describe("previewNextNumber", () => {
  it("zero-pads the counter to the configured width", () => {
    expect(previewNextNumber("SINV-", 5, "NEVER", 1, 1, new Date(2026, 0, 1))).toBe("SINV-00001");
    expect(previewNextNumber("SINV-", 5, "NEVER", 1, 123456, new Date(2026, 0, 1))).toBe("SINV-123456");
  });

  it("ignores fiscalYearStartMonth when the reset policy is NEVER", () => {
    expect(previewNextNumber("INV-{YYYY}-", 3, "NEVER", 4, 1, new Date(2026, 2, 1))).toBe("INV-2026-001");
  });

  it("applies the fiscal-year label when the reset policy is ANNUAL", () => {
    expect(previewNextNumber("GRN-{FY}-", 5, "ANNUAL", 4, 1, new Date(2026, 2, 31))).toBe("GRN-2025-26-00001");
  });
});
