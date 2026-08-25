import createClient from "openapi-fetch";

import type { paths } from "./schema";

const baseUrl = (
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api"
).replace(/\/$/, "");

/**
 * Typed fetch client generated against the backend's OpenAPI contract
 * (F0.0.2). Auth/refresh handling (rotating refresh tokens, IAM-1) is
 * layered on in F0.2.1 — this is intentionally the bare typed transport.
 */
export const apiClient = createClient<paths>({ baseUrl });
