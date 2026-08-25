"use client";

import Link from "next/link";

import { cn } from "@/lib/utils";
import { NAV_ITEMS } from "./nav-items";

interface NavProps {
  open: boolean;
  onClose: () => void;
}

export function Nav({ open, onClose }: NavProps) {
  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-30 bg-black/40 lg:hidden"
          aria-hidden="true"
          onClick={onClose}
        />
      )}
      <nav
        aria-label="Primary"
        className={cn(
          "fixed top-14 bottom-0 left-0 z-40 w-64 -translate-x-full overflow-y-auto border-r bg-sidebar p-3 transition-transform duration-200 ease-in-out",
          "lg:static lg:z-0 lg:h-auto lg:w-56 lg:translate-x-0",
          open && "translate-x-0",
        )}
      >
        <ul className="flex flex-col gap-1">
          {NAV_ITEMS.map((item) => (
            <li key={item.label}>
              {item.href ? (
                <Link
                  href={item.href}
                  onClick={onClose}
                  className="block rounded-md px-3 py-2 text-sm font-medium text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                >
                  {item.label}
                </Link>
              ) : (
                <span
                  aria-disabled="true"
                  className="block cursor-not-allowed rounded-md px-3 py-2 text-sm text-muted-foreground"
                >
                  {item.label}
                </span>
              )}
            </li>
          ))}
        </ul>
      </nav>
    </>
  );
}
