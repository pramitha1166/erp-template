import { apiFetch } from "./http";

/** Mirrors `AuthController.LoginRequest`. */
export interface LoginRequest {
  tenantId: string;
  email: string;
  password: string;
}

/** Mirrors `AuthController.LoginResponse`. */
export interface LoginResponse {
  mfaRequired: boolean;
  mfaChallengeToken: string | null;
  accessToken: string | null;
  refreshToken: string | null;
  passwordChangeRequired: boolean;
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/auth/login", { method: "POST", body: request, auth: false });
}

/** Mirrors `AdminAuthController.LoginRequest` (ADM-1 / ADM-5, F0.11.7) — no `tenantId`: platform/brand admin staff have no tenant of their own to supply. */
export interface AdminLoginRequest {
  email: string;
  password: string;
}

/** Same response shape as tenant login (`AdminAuthController.LoginResponse`); the admin realm's `tid` is fixed server-side. */
export function adminLogin(request: AdminLoginRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/admin/auth/login", { method: "POST", body: request, auth: false });
}

export interface TotpVerifyRequest {
  mfaChallengeToken: string;
  code: string;
}

export function verifyTotp(request: TotpVerifyRequest): Promise<LoginResponse> {
  return apiFetch<LoginResponse>("/auth/totp/verify", { method: "POST", body: request, auth: false });
}

export function logout(refreshToken: string): Promise<void> {
  return apiFetch<void>("/auth/logout", { method: "POST", body: { refreshToken }, auth: false });
}

/** Mirrors `AuthController.RefreshResponse`. Used to restore a session on app boot from a stored refresh token. */
export interface RefreshResponse {
  accessToken: string;
  refreshToken: string;
}

export function refresh(refreshToken: string): Promise<RefreshResponse> {
  return apiFetch<RefreshResponse>("/auth/refresh", { method: "POST", body: { refreshToken }, auth: false });
}

export interface TotpSetupResponse {
  secret: string;
  otpAuthUri: string;
}

export function setupTotp(): Promise<TotpSetupResponse> {
  return apiFetch<TotpSetupResponse>("/auth/totp/setup", { method: "POST" });
}

export function enableTotp(code: string): Promise<void> {
  return apiFetch<void>("/auth/totp/enable", { method: "POST", body: { code } });
}

export function disableTotp(): Promise<void> {
  return apiFetch<void>("/auth/totp/disable", { method: "POST" });
}

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return apiFetch<void>("/auth/password/change", {
    method: "POST",
    body: { currentPassword, newPassword },
  });
}

/** Mirrors `AuthController.SessionView`. */
export interface SessionView {
  id: string;
  issuedAt: string;
  lastSeenAt: string;
  ipAddress: string | null;
  userAgent: string | null;
}

export function listSessions(): Promise<SessionView[]> {
  return apiFetch<SessionView[]>("/auth/sessions");
}

export function revokeSession(sessionId: string): Promise<void> {
  return apiFetch<void>(`/auth/sessions/${sessionId}`, { method: "DELETE" });
}
