import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setAccessToken,
  setRefreshToken,
} from "./tokens";

const baseUrl = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api"
).replace(/\/$/, "");

/** Shape of `ApiError` from `IamExceptionHandler` (and every other module's error advice). */
export class ApiError extends Error {
  readonly status: number;
  readonly details: string[];

  constructor(status: number, message: string, details: string[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.details = details;
  }
}

export interface ApiRequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  query?: Record<string, string | undefined>;
  /** Skip attaching a bearer token and skip the refresh-and-retry-on-401 dance. Default true. */
  auth?: boolean;
}

let inFlightRefresh: Promise<boolean> | null = null;

/**
 * F0.2.1 / IAM-1: rotates the refresh token exactly once for however many
 * requests hit a 401 concurrently — without this lock, two racing 401s
 * would each rotate the token and the loser's retry would fail with a
 * reused (and therefore fully revoked, per `SessionService.rotate`)
 * refresh token.
 */
function refreshSessionOnce(): Promise<boolean> {
  if (!inFlightRefresh) {
    inFlightRefresh = performRefresh().finally(() => {
      inFlightRefresh = null;
    });
  }
  return inFlightRefresh;
}

async function performRefresh(): Promise<boolean> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) {
    return false;
  }
  const response = await fetch(`${baseUrl}/auth/refresh`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
  });
  if (!response.ok) {
    clearTokens();
    return false;
  }
  const pair = (await response.json()) as {
    accessToken: string;
    refreshToken: string;
  };
  setAccessToken(pair.accessToken);
  setRefreshToken(pair.refreshToken);
  return true;
}

function buildUrl(path: string, query?: Record<string, string | undefined>): string {
  const url = new URL(baseUrl + path);
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined) {
      url.searchParams.set(key, value);
    }
  }
  return url.toString();
}

async function parseError(response: Response): Promise<ApiError> {
  try {
    const data = (await response.json()) as { message?: string; details?: string[] };
    return new ApiError(response.status, data.message ?? response.statusText, data.details ?? []);
  } catch {
    return new ApiError(response.status, response.statusText);
  }
}

/**
 * Typed transport for the auth/IAM endpoints (F0.2.1). This is deliberately
 * separate from `apiClient` in `client.ts`: that client's `paths` type is
 * still the F0.0.2 placeholder (regenerating it needs a live backend build
 * against Postgres, which isn't available to hand-author this PR), so it
 * can't type-check calls to endpoints that exist today. `auth-api.ts` and
 * `iam-api.ts` hand-author their request/response shapes straight from the
 * controllers instead. Once `npm run generate:api` is run for real, callers
 * should migrate to `apiClient` and this module should shrink to just the
 * refresh-retry plumbing `apiClient`'s `use()` hooks would call into.
 */
export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { method = "GET", body, query, auth = true } = options;

  const send = () => {
    const headers: Record<string, string> = {};
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }
    if (auth) {
      const token = getAccessToken();
      if (token) {
        headers.Authorization = `Bearer ${token}`;
      }
    }
    return fetch(buildUrl(path, query), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  };

  let response = await send();

  if (response.status === 401 && auth && getRefreshToken()) {
    const refreshed = await refreshSessionOnce();
    if (refreshed) {
      response = await send();
    }
  }

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
