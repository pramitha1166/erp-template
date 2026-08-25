import { describe, expect, it } from "vitest";

import { passwordPolicyChecklist } from "./password-policy";
import type { SecurityPolicy } from "@/lib/api/iam-api";

const policy: SecurityPolicy = {
  idleTimeoutMinutes: 30,
  minLength: 10,
  requireUpper: true,
  requireLower: true,
  requireDigit: true,
  requireSymbol: false,
  historyCount: 3,
  expiryDays: null,
};

describe("passwordPolicyChecklist", () => {
  it("flags every unmet rule for an empty password", () => {
    const rules = passwordPolicyChecklist(policy, "");

    expect(rules.every((rule) => !rule.met)).toBe(true);
  });

  it("marks every rule met once a password satisfies the policy", () => {
    const rules = passwordPolicyChecklist(policy, "Abcdefg123");

    expect(rules.every((rule) => rule.met)).toBe(true);
  });

  it("only checks the length rule when nothing else is required", () => {
    const laxPolicy: SecurityPolicy = { ...policy, requireUpper: false, requireLower: false, requireDigit: false };

    const rules = passwordPolicyChecklist(laxPolicy, "lowercaseonly");

    expect(rules).toHaveLength(1);
    expect(rules[0].met).toBe(true);
  });

  it("requires a non-alphanumeric character when requireSymbol is set", () => {
    const symbolPolicy: SecurityPolicy = { ...policy, requireSymbol: true };

    expect(passwordPolicyChecklist(symbolPolicy, "Abcdefg123").find((r) => r.label === "A symbol")?.met).toBe(false);
    expect(passwordPolicyChecklist(symbolPolicy, "Abcdefg123!").find((r) => r.label === "A symbol")?.met).toBe(true);
  });
});
