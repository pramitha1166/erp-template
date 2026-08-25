import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { apiFetch, ApiError } from "./http";
import { clearTokens, getAccessToken, getRefreshToken, setAccessToken, setRefreshToken } from "./tokens";

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

describe("apiFetch", () => {
  beforeEach(() => {
    clearTokens();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("returns parsed JSON on success", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiFetch<{ ok: boolean }>("/ping", { auth: false });

    expect(result).toEqual({ ok: true });
  });

  it("attaches a bearer token when one is set", async () => {
    setAccessToken("token-123");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({}));
    vi.stubGlobal("fetch", fetchMock);

    await apiFetch("/secure");

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect((init.headers as Record<string, string>).Authorization).toBe("Bearer token-123");
  });

  it("F0.2.1: refreshes exactly once on a 401 and retries the original request", async () => {
    setAccessToken("expired");
    setRefreshToken("refresh-1");

    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ message: "Unauthorized" }, 401))
      .mockResolvedValueOnce(jsonResponse({ accessToken: "fresh", refreshToken: "refresh-2" }))
      .mockResolvedValueOnce(jsonResponse({ data: "ok" }));
    vi.stubGlobal("fetch", fetchMock);

    const result = await apiFetch<{ data: string }>("/secure");

    expect(result).toEqual({ data: "ok" });
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(getAccessToken()).toBe("fresh");
    expect(getRefreshToken()).toBe("refresh-2");
  });

  it("does not attempt a refresh when there is no refresh token stored", async () => {
    setAccessToken("expired");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ message: "Unauthorized" }, 401));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/secure")).rejects.toBeInstanceOf(ApiError);
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("clears the stored session when the refresh call itself fails", async () => {
    setAccessToken("expired");
    setRefreshToken("refresh-1");
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ message: "Unauthorized" }, 401))
      .mockResolvedValueOnce(jsonResponse({ message: "Invalid refresh token" }, 401));
    vi.stubGlobal("fetch", fetchMock);

    await expect(apiFetch("/secure")).rejects.toBeInstanceOf(ApiError);
    expect(getRefreshToken()).toBeNull();
  });

  it("surfaces the ApiError message and details from the backend's error body", async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValue(jsonResponse({ message: "Validation failed", details: ["newPassword: too short"] }, 422));
    vi.stubGlobal("fetch", fetchMock);

    const error = await apiFetch("/anything", { auth: false }).catch((caught: unknown) => caught);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).status).toBe(422);
    expect((error as ApiError).message).toBe("Validation failed");
    expect((error as ApiError).details).toEqual(["newPassword: too short"]);
  });
});
