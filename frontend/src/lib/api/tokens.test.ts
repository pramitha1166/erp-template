import { afterEach, describe, expect, it } from "vitest";

import { clearTokens, getAccessToken, getRefreshToken, setAccessToken, setRefreshToken } from "./tokens";

describe("tokens", () => {
  afterEach(() => {
    clearTokens();
  });

  it("keeps the access token in memory, separate from the refresh token", () => {
    setAccessToken("access-1");
    setRefreshToken("refresh-1");

    expect(getAccessToken()).toBe("access-1");
    expect(getRefreshToken()).toBe("refresh-1");
  });

  it("persists the refresh token to localStorage", () => {
    setRefreshToken("refresh-1");

    expect(window.localStorage.getItem("erp.refreshToken")).toBe("refresh-1");
  });

  it("removes the refresh token from storage when set to null", () => {
    setRefreshToken("refresh-1");
    setRefreshToken(null);

    expect(window.localStorage.getItem("erp.refreshToken")).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });

  it("clearTokens wipes both the in-memory access token and the stored refresh token", () => {
    setAccessToken("access-1");
    setRefreshToken("refresh-1");

    clearTokens();

    expect(getAccessToken()).toBeNull();
    expect(getRefreshToken()).toBeNull();
  });
});
