import { describe, expect, it } from "vitest";

import { safeNext } from "./safe-redirect";

describe("safeNext", () => {
  it("returns null for a missing value", () => {
    expect(safeNext(null)).toBeNull();
    expect(safeNext(undefined)).toBeNull();
    expect(safeNext("")).toBeNull();
  });

  it("accepts an in-app relative path", () => {
    expect(safeNext("/admin/roles")).toBe("/admin/roles");
  });

  it("rejects a protocol-relative URL (open-redirect attempt)", () => {
    expect(safeNext("//evil.example.com")).toBeNull();
  });

  it("rejects an absolute URL", () => {
    expect(safeNext("https://evil.example.com")).toBeNull();
  });

  it("rejects a path that doesn't start with a slash", () => {
    expect(safeNext("evil.example.com")).toBeNull();
  });
});
