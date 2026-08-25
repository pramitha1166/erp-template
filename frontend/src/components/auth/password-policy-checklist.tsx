import { Check, X } from "lucide-react";

import { cn } from "@/lib/utils";
import { passwordPolicyChecklist } from "@/lib/auth/password-policy";
import type { SecurityPolicy } from "@/lib/api/iam-api";

export interface PasswordPolicyChecklistProps {
  policy: SecurityPolicy;
  value: string;
}

/** F0.2.3 / IAM-9: live feedback against the tenant's configured password policy as the operator types. */
export function PasswordPolicyChecklist({ policy, value }: PasswordPolicyChecklistProps) {
  const rules = passwordPolicyChecklist(policy, value);

  return (
    <ul className="flex flex-col gap-1 text-sm">
      {rules.map((rule) => (
        <li
          key={rule.label}
          className={cn(
            "flex items-center gap-1.5",
            rule.met ? "text-foreground" : "text-muted-foreground",
          )}
        >
          {rule.met ? (
            <Check className="size-3.5 shrink-0 text-primary" aria-hidden="true" />
          ) : (
            <X className="size-3.5 shrink-0" aria-hidden="true" />
          )}
          {rule.label}
        </li>
      ))}
    </ul>
  );
}
