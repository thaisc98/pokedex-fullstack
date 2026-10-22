"use client";

import { Alert, Button, toast } from "@heroui/react";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { baseApi } from "@/shared/api/baseApi";
import { clearSignedInHint } from "@/shared/api/cookies";
import { safeRedirect } from "@/shared/lib/safeRedirect";
import { hasUnsavedChanges, subscribeUnsavedChanges } from "@/shared/lib/unsavedChanges";
import { AppButtonLink } from "@/shared/ui/AppLink";
import { useAppDispatch, useAppSelector } from "@/store/hooks";
import { selectUser, sessionEnded } from "./authSlice";

/** The warning shows this long before the session ends (security.md §5). */
export const WARNING_LEAD_MS = 2 * 60 * 1000;

type Notice = { kind: "warning"; for: string } | { kind: "expired" } | null;

function isProtected(pathname: string) {
  return pathname === "/collection" || pathname.startsWith("/collection/");
}

/**
 * Session expiry (security.md §5). There is no refresh token, so the app can only warn and
 * then sign out cleanly:
 * - 2 minutes before `expiresAt`: a non-blocking notice with "Log in again" (once per session);
 * - at `expiresAt`: the same clean-up as a 401 (auth state, RTK Query cache, signed-in hint),
 *   "You've been signed out", and protected pages go to /login?from=….
 * If a form has unsaved input at expiry, nothing is thrown away: the page stays, the notice
 * says to copy the changes first, and the clean-up finishes once the input is gone.
 *
 * Timers come from `expiresAt` vs `Date.now()`, are re-armed whenever the session changes
 * (login, /auth/me, logout) and re-checked when the tab becomes visible (timers can be late
 * after the device sleeps). No polling.
 */
export function SessionExpiry() {
  const dispatch = useAppDispatch();
  const router = useRouter();
  const pathname = usePathname();
  const user = useAppSelector(selectUser);
  const expiresAt = useAppSelector((state) => state.auth.expiresAt);
  const [notice, setNotice] = useState<Notice>(null);
  // Once per session: remembers which expiresAt was already warned about (survives re-renders).
  const warnedFor = useRef<string | null>(null);

  useEffect(() => {
    if (!user || !expiresAt) return undefined;
    const end = Date.parse(expiresAt);
    if (!Number.isFinite(end)) return undefined;

    let warnTimer: ReturnType<typeof setTimeout> | undefined;
    let endTimer: ReturnType<typeof setTimeout> | undefined;
    let stopWaitingForInput: (() => void) | undefined;

    const finishSignOut = () => {
      dispatch(baseApi.util.resetApiState());
      setNotice(null);
      toast.warning("You’ve been signed out", { description: "Your session ended. Log in again to continue." });
      const here = window.location.pathname;
      if (isProtected(here)) {
        router.replace(`/login?from=${encodeURIComponent(safeRedirect(here + window.location.search))}`);
      }
    };

    const expire = () => {
      clearTimeout(warnTimer);
      clearSignedInHint();
      dispatch(sessionEnded());
      if (!hasUnsavedChanges()) {
        finishSignOut();
        return;
      }
      // Keep the page and its input; finish once the input is saved elsewhere or discarded.
      setNotice({ kind: "expired" });
      stopWaitingForInput = subscribeUnsavedChanges(() => {
        if (!hasUnsavedChanges()) {
          stopWaitingForInput?.();
          finishSignOut();
        }
      });
    };

    const warn = () => {
      warnedFor.current = expiresAt;
      setNotice({ kind: "warning", for: expiresAt });
    };

    const arm = () => {
      clearTimeout(warnTimer);
      clearTimeout(endTimer);
      const left = end - Date.now();
      if (warnedFor.current !== expiresAt) warnTimer = setTimeout(warn, Math.max(0, left - WARNING_LEAD_MS));
      endTimer = setTimeout(expire, Math.max(0, left));
    };

    const onVisibilityChange = () => {
      if (document.visibilityState === "visible") arm();
    };

    arm();
    document.addEventListener("visibilitychange", onVisibilityChange);
    return () => {
      clearTimeout(warnTimer);
      clearTimeout(endTimer);
      stopWaitingForInput?.();
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, [dispatch, router, user, expiresAt]);

  // A warning belongs to one session: a new login (new expiresAt) or a logout hides it.
  const showWarning = notice?.kind === "warning" && user !== null && notice.for === expiresAt;
  const showExpired = notice?.kind === "expired";
  if (!showWarning && !showExpired) return null;

  // Rendered only after a timer fired, i.e. in the browser.
  const loginHref = `/login?from=${encodeURIComponent(safeRedirect(pathname + window.location.search, "/pokemon"))}`;

  return (
    <div className="fixed inset-x-4 bottom-4 z-50 sm:left-auto sm:max-w-md" role="status" aria-live="polite">
      <Alert status={showExpired ? "danger" : "warning"} className="shadow-overlay">
        <Alert.Indicator />
        <Alert.Content className="gap-2">
          {/* Foreground text: the warning tint as text is below 4.5:1; the icon carries the tone. */}
          <Alert.Title className="text-foreground">
            {showExpired ? "You’ve been signed out" : "Your session ends in 2 minutes. Save your changes."}
          </Alert.Title>
          {showExpired && (
            <Alert.Description>
              Your changes are still on this page but can’t be saved. Copy them, then log in again.
            </Alert.Description>
          )}
          <div className="flex flex-wrap gap-2">
            <AppButtonLink href={loginHref} size="sm">
              Log in again
            </AppButtonLink>
            <Button size="sm" variant="ghost" onPress={() => setNotice(null)}>
              Dismiss
            </Button>
          </div>
        </Alert.Content>
      </Alert>
    </div>
  );
}
