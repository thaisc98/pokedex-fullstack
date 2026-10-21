"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useSession } from "./useSession";

/**
 * Client-side guard for protected pages (UX only; the API enforces every rule).
 * Shows `fallback` until the session is known; signed out → /login?from=<here>.
 */
export function RequireAuth({ children, fallback }: { children: ReactNode; fallback: ReactNode }) {
  const { ready: checked, user } = useSession();
  const router = useRouter();
  const pathname = usePathname();
  const search = useSearchParams().toString();

  useEffect(() => {
    if (checked && !user) {
      const from = `${pathname}${search ? `?${search}` : ""}`;
      router.replace(`/login?from=${encodeURIComponent(from)}`);
    }
  }, [checked, user, pathname, search, router]);

  if (!checked || !user) return <>{fallback}</>;
  return <>{children}</>;
}
