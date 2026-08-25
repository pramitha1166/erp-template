/**
 * IAM-2: the 5-minute-lived `mfaChallengeToken` `AuthController` hands back
 * between "password checked out" and "TOTP code verified". Held in memory
 * only (like the access token in `tokens.ts`) and scoped to a single
 * client-side navigation from `/login` to `/totp-verify` — there's nothing
 * to gain from persisting a token this short-lived across a reload.
 */
interface PendingChallenge {
  mfaChallengeToken: string;
  email: string;
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
