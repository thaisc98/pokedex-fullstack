"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useAppSelector } from "@/store/hooks";
import { selectAuthChecked, selectUser } from "./authSlice";

/**
 * Client-side guard for protected pages (UX only; the API enforces every rule).
 * Shows `fallback` until the session is known; signed out → /login?from=<here>.
 */
export function RequireAuth({ children, fallback }: { children: ReactNode; fallback: ReactNode }) {
  const checked = useAppSelector(selectAuthChecked);
  const user = useAppSelector(selectUser);
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
