import { describe, expect, it } from "vitest";

import { formatFileSize } from "./browser-file";

describe("formatFileSize", () => {
  it("renders bytes below 1024 without a decimal", () => {
    expect(formatFileSize(512)).toBe("512 B");
  });

  it.each([
    [1536, "1.5 KB"],
    [1024 * 1024 * 2.5, "2.5 MB"],
    [1024 * 1024 * 1024 * 3, "3.0 GB"],
  ])("formats %d bytes as %s", (bytes, expected) => {
    expect(formatFileSize(bytes)).toBe(expected);
  });
});
