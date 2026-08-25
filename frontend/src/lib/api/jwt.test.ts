import { describe, expect, it } from "vitest";

import { decodeAccessToken } from "./jwt";

function fakeToken(payload: Record<string, unknown>): string {
  const base64Url = (value: string) =>
    Buffer.from(value).toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  const header = base64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const body = base64Url(JSON.stringify(payload));
  return `${header}.${body}.signature`;
}

describe("decodeAccessToken", () => {
  it("reads the userId (sub) and tenantId (tid) claims", () => {
    const token = fakeToken({ sub: "user-1", tid: "tenant-1", purpose: "access" });

    expect(decodeAccessToken(token)).toEqual({ userId: "user-1", tenantId: "tenant-1" });
  });

  it("returns null for a malformed token", () => {
    expect(decodeAccessToken("not-a-jwt")).toBeNull();
  });

  it("returns null when required claims are missing", () => {
    const token = fakeToken({ sub: "user-1" });

    expect(decodeAccessToken(token)).toBeNull();
  });
});
