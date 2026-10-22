"use client";

import { usePathname } from "next/navigation";
import { useEffect, useRef } from "react";
import { clearSignedInHint, hasSignedInHint } from "@/shared/api/cookies";
import { useAppDispatch } from "@/store/hooks";
import { authApi } from "./authApi";
import { sessionEnded } from "./authSlice";

/** Pages where we never need to restore a session: the user is about to sign in anyway. */
const AUTH_PAGES = new Set(["/login", "/register"]);

/**
 * Restores the session on startup (architecture.md §3.4): `GET /auth/me` when we were
 * signed in before; visitors skip the call (it would only log a 401 in the console).
 *
 * On /login and /register the probe is skipped and the hint dropped: a hint can outlive
 * its token (e.g. the server was reset), and probing there would log a 401 right before
 * the user signs in again. Logging in sets a fresh hint.
 */
export function SessionBootstrap() {
  const dispatch = useAppDispatch();
  const pathname = usePathname();
  const started = useRef(false);

  useEffect(() => {
    if (started.current) return undefined;
    started.current = true;
    if (hasSignedInHint() && !AUTH_PAGES.has(pathname)) {
      const request = dispatch(authApi.endpoints.me.initiate());
      return () => request.unsubscribe();
    }
    if (AUTH_PAGES.has(pathname)) clearSignedInHint();
    dispatch(sessionEnded());
    return undefined;
  }, [dispatch, pathname]);

  return null;
}
