import type { SecurityPolicy } from "@/lib/api/iam-api";

export interface PasswordRule {
  label: string;
  met: boolean;
}

/**
 * IAM-9: mirrors `PasswordPolicyService.complexityViolations` exactly (see
 * backend) so the client-side hint and the server-side rejection never
 * disagree about what "valid" means. This is a live hint only — the server
 * re-validates on submit regardless (including password-history reuse,
 * which has no client-side equivalent since raw history isn't exposed).
 */
export function passwordPolicyChecklist(policy: SecurityPolicy, value: string): PasswordRule[] {
  const rules: PasswordRule[] = [
    { label: `At least ${policy.minLength} characters`, met: value.length >= policy.minLength },
  ];
  if (policy.requireUpper) {
    rules.push({ label: "An uppercase letter", met: /[A-Z]/.test(value) });
  }
  if (policy.requireLower) {
    rules.push({ label: "A lowercase letter", met: /[a-z]/.test(value) });
  }
  if (policy.requireDigit) {
    rules.push({ label: "A digit", met: /[0-9]/.test(value) });
  }
  if (policy.requireSymbol) {
    rules.push({ label: "A symbol", met: value.length > 0 && !/^[a-zA-Z0-9]*$/.test(value) });
  }
  return rules;
}
