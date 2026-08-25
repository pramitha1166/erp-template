import { describe, expect, it } from "vitest";

import { cn } from "./utils";

describe("cn", () => {
  it("joins truthy class values", () => {
    expect(cn("a", false, undefined, "b")).toBe("a b");
  });

  it("merges conflicting Tailwind classes, keeping the last one", () => {
    expect(cn("px-2", "px-4")).toBe("px-4");
  });
});
