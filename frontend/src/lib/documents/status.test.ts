import { describe, expect, it } from "vitest";

import { canTransition } from "./status";

describe("canTransition", () => {
  it("allows DRAFT to submit", () => {
    expect(canTransition("DRAFT", "submit")).toBe(true);
  });

  it("does not allow DRAFT to cancel or amend", () => {
    expect(canTransition("DRAFT", "cancel")).toBe(false);
    expect(canTransition("DRAFT", "amend")).toBe(false);
  });

  it("allows SUBMITTED to cancel or amend, but not resubmit", () => {
    expect(canTransition("SUBMITTED", "cancel")).toBe(true);
    expect(canTransition("SUBMITTED", "amend")).toBe(true);
    expect(canTransition("SUBMITTED", "submit")).toBe(false);
  });

  it("treats CANCELLED and AMENDED as terminal", () => {
    for (const action of ["submit", "cancel", "amend"] as const) {
      expect(canTransition("CANCELLED", action)).toBe(false);
      expect(canTransition("AMENDED", action)).toBe(false);
    }
  });
});
