"use client";

import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { UserRound } from "lucide-react";

import { endSession } from "@/lib/auth/session";
import { useSessionStore } from "@/stores/session-store";
import { Button } from "@/components/ui/button";

/** F0.2.1 / F0.2.2 / F0.2.4: the account menu — sign out, and the entry points into the account-security screens. */
export function SessionMenu() {
  const user = useSessionStore((state) => state.user);
  const router = useRouter();
  const [open, setOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) {
      return;
    }
    function handlePointerDown(event: PointerEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [open]);

  if (!user) {
    return null;
  }

  async function handleSignOut() {
    await endSession();
    router.replace("/login");
  }

  return (
    <div ref={containerRef} className="relative">
      <Button
        variant="ghost"
        size="sm"
        className="gap-1.5"
        aria-haspopup="true"
        aria-expanded={open}
        onClick={() => setOpen((value) => !value)}
      >
        <UserRound className="size-4" aria-hidden="true" />
        {user.displayName}
      </Button>
      {open && (
        <div
          role="menu"
          className="absolute top-full right-0 z-50 mt-1 w-56 rounded-md border bg-popover p-1 text-popover-foreground shadow-md"
        >
          <Link
            role="menuitem"
            href="/account/two-factor"
            className="block rounded-sm px-2 py-1.5 text-sm hover:bg-accent"
            onClick={() => setOpen(false)}
          >
            Two-factor authentication
          </Link>
          <Link
            role="menuitem"
            href="/account/sessions"
            className="block rounded-sm px-2 py-1.5 text-sm hover:bg-accent"
            onClick={() => setOpen(false)}
          >
            Active sessions
          </Link>
          <Link
            role="menuitem"
            href="/account/change-password"
            className="block rounded-sm px-2 py-1.5 text-sm hover:bg-accent"
            onClick={() => setOpen(false)}
          >
            Change password
          </Link>
          <Link
            role="menuitem"
            href="/account/delegations"
            className="block rounded-sm px-2 py-1.5 text-sm hover:bg-accent"
            onClick={() => setOpen(false)}
          >
            Approval delegation
          </Link>
          <button
            type="button"
            role="menuitem"
            className="block w-full rounded-sm px-2 py-1.5 text-left text-sm hover:bg-accent"
            onClick={handleSignOut}
          >
            Sign out
          </button>
        </div>
      )}
    </div>
  );
}
