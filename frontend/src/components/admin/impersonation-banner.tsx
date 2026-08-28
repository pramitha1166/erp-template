"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

import { endImpersonation } from "@/lib/auth/impersonation";
import { useImpersonationStore } from "@/stores/impersonation-store";
import { Button } from "@/components/ui/button";

function formatRemaining(expiresAt: string): string {
  const remainingMs = new Date(expiresAt).getTime() - Date.now();
  if (remainingMs <= 0) {
    return "0:00";
  }
  const totalSeconds = Math.floor(remainingMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

/**
 * F0.11.5 / ADM-7: persistent, impossible-to-miss banner for the duration
 * of an impersonated session — mounted once in `AppShell` so it survives
 * navigation between screens. Auto-ends the session client-side when the
 * 30-minute window (`ImpersonationService.SESSION_TTL`) runs out; the
 * backend enforces the real expiry independently via the token itself.
 */
export function ImpersonationBanner() {
  const router = useRouter();
  const active = useImpersonationStore((state) => state.active);
  const tenantName = useImpersonationStore((state) => state.tenantName);
  const expiresAt = useImpersonationStore((state) => state.expiresAt);
  const [remaining, setRemaining] = useState("");
  const [ending, setEnding] = useState(false);

  useEffect(() => {
    if (!active || !expiresAt) {
      return;
    }
    const tick = () => {
      setRemaining(formatRemaining(expiresAt));
      if (new Date(expiresAt).getTime() - Date.now() <= 0) {
        void handleEnd();
      }
    };
    tick();
    const interval = window.setInterval(tick, 1000);
    return () => window.clearInterval(interval);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [active, expiresAt]);

  async function handleEnd() {
    setEnding(true);
    try {
      await endImpersonation();
      router.replace("/");
    } finally {
      setEnding(false);
    }
  }

  if (!active) {
    return null;
  }

  return (
    <div
      role="status"
      className="flex flex-wrap items-center justify-center gap-3 bg-amber-500 px-4 py-2 text-sm font-medium text-amber-950"
    >
      <span>
        Impersonating <strong>{tenantName}</strong> — ends in {remaining}
      </span>
      <Button type="button" size="sm" variant="outline" disabled={ending} onClick={handleEnd}>
        {ending ? "Ending…" : "End impersonation"}
      </Button>
    </div>
  );
}
