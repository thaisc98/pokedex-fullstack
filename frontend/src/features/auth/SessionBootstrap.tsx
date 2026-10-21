"use client";

import { useEffect } from "react";
import { hasSignedInHint } from "@/shared/api/cookies";
import { useAppDispatch } from "@/store/hooks";
import { authApi } from "./authApi";
import { sessionEnded } from "./authSlice";

/**
 * Restores the session on startup (architecture.md §3.4): `GET /auth/me` when we were
 * signed in before; visitors skip the call (it would only log a 401 in the console).
 */
export function SessionBootstrap() {
  const dispatch = useAppDispatch();
  useEffect(() => {
    if (hasSignedInHint()) {
      const request = dispatch(authApi.endpoints.me.initiate());
      return () => request.unsubscribe();
    }
    dispatch(sessionEnded());
    return undefined;
  }, [dispatch]);
  return null;
}
