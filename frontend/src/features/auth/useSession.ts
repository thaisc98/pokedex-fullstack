"use client";

import { useIsClient } from "@/shared/lib/useIsClient";
import { useAppSelector } from "@/store/hooks";
import { selectAuthChecked, selectUser } from "./authSlice";

/**
 * Session for rendering. `ready` stays false until THIS component has hydrated: streamed
 * page segments can hydrate after `GET /auth/me` already resolved, and rendering the
 * signed-in UI during their hydration would not match the server HTML (hydration error).
 */
export function useSession() {
  const hydrated = useIsClient();
  const checked = useAppSelector(selectAuthChecked);
  const user = useAppSelector(selectUser);
  const ready = hydrated && checked;
  return { ready, user: ready ? user : null };
}
