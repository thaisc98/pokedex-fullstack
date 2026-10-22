"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useHasUnsavedChanges } from "@/shared/lib/unsavedChanges";
import { useSession } from "./useSession";

/**
 * Client-side guard for protected pages (UX only; the API enforces every rule).
 * Shows `fallback` until the session is known; signed out → /login?from=<here>.
 *
 * One exception: if the session ends while a form on the page has unsaved input, the page
 * stays as it is (no redirect, no unmount) so nothing is lost silently; SessionExpiry tells
 * the user to copy their changes and log in again. The redirect happens once the input is
 * saved elsewhere or discarded.
 */
export function RequireAuth({ children, fallback }: { children: ReactNode; fallback: ReactNode }) {
  const { ready: checked, user } = useSession();
  const unsaved = useHasUnsavedChanges();
  const router = useRouter();
  const pathname = usePathname();
  const search = useSearchParams().toString();
  // A dirty form only exists on a protected page if the user was signed in when it rendered.
  const keepForUnsaved = checked && !user && unsaved;

  useEffect(() => {
    if (checked && !user && !keepForUnsaved) {
      const from = `${pathname}${search ? `?${search}` : ""}`;
      router.replace(`/login?from=${encodeURIComponent(from)}`);
    }
  }, [checked, user, keepForUnsaved, pathname, search, router]);

  if (keepForUnsaved) return <>{children}</>;
  if (!checked || !user) return <>{fallback}</>;
  return <>{children}</>;
}
