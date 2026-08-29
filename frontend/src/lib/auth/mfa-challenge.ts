/**
 * IAM-2: the 5-minute-lived `mfaChallengeToken` `AuthController` hands back
 * between "password checked out" and "TOTP code verified". Held in memory
 * only (like the access token in `tokens.ts`) and scoped to a single
 * client-side navigation from `/login` (or, for platform/brand admin
 * staff, `/admin-login`) to `/totp-verify` — there's nothing to gain from
 * persisting a token this short-lived across a reload.
 *
 * `defaultNext` lets one shared `/totp-verify` step (the backend's
 * `AuthService.verifyTotp` already doesn't care which realm issued the
 * challenge — see `AdminAuthController`'s javadoc) send each realm to its
 * own post-login landing page when the caller didn't ask for a specific
 * one, instead of always assuming the tenant realm's `/`.
 */
interface PendingChallenge {
  mfaChallengeToken: string;
  email: string;
  defaultNext: string;
}

let pending: PendingChallenge | null = null;

export function setPendingChallenge(challenge: PendingChallenge): void {
  pending = challenge;
}

export function getPendingChallenge(): PendingChallenge | null {
  return pending;
}

export function clearPendingChallenge(): void {
  pending = null;
}
