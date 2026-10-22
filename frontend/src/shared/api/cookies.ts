/**
 * Browser cookie helpers. Only for cookies JavaScript is MEANT to read:
 * - `XSRF-TOKEN` (set by Spring, csrf.spa(); security.md §4.2.1)
 * - `pokedex_signed_in` (a UX hint we set ourselves; see below)
 * The JWT lives in the httpOnly `access_token` cookie and is never visible here.
 */

export const CSRF_COOKIE = "XSRF-TOKEN";
export const CSRF_HEADER = "X-XSRF-TOKEN";

/**
 * "Probably signed in" hint, set after login and cleared on logout / 401.
 * It holds no secret and grants nothing: `access_token` is `Path=/api`, so neither
 * proxy.ts (page requests) nor JS can see it. The hint lets proxy.ts do its optimistic
 * /collection redirect and lets the app skip `GET /auth/me` for visitors (which would
 * otherwise log a 401 in the console on every page load).
 */
export const SIGNED_IN_HINT_COOKIE = "pokedex_signed_in";

export function readCookie(name: string): string | undefined {
  if (typeof document === "undefined") return undefined;
  for (const part of document.cookie.split(";")) {
    const [rawKey, ...rest] = part.trim().split("=");
    if (rawKey === name) return decodeURIComponent(rest.join("="));
  }
  return undefined;
}

/**
 * When the session ends (ISO-8601, from the login response). `GET /auth/me` doesn't return
 * it, so it is kept next to the hint to re-arm the expiry warning after a reload. Not a secret.
 */
export const SESSION_EXPIRES_COOKIE = "pokedex_session_expires";

export function setSignedInHint(expiresAt: string | null) {
  if (typeof document === "undefined") return;
  const seconds = expiresAt ? Math.floor((Date.parse(expiresAt) - Date.now()) / 1000) : 3600;
  const maxAge = Number.isFinite(seconds) && seconds > 0 ? seconds : 3600;
  document.cookie = `${SIGNED_IN_HINT_COOKIE}=1; Path=/; Max-Age=${maxAge}; SameSite=Strict`;
  if (expiresAt && Number.isFinite(Date.parse(expiresAt))) {
    document.cookie = `${SESSION_EXPIRES_COOKIE}=${encodeURIComponent(expiresAt)}; Path=/; Max-Age=${maxAge}; SameSite=Strict`;
  }
}

export function clearSignedInHint() {
  if (typeof document === "undefined") return;
  document.cookie = `${SIGNED_IN_HINT_COOKIE}=; Path=/; Max-Age=0; SameSite=Strict`;
  document.cookie = `${SESSION_EXPIRES_COOKIE}=; Path=/; Max-Age=0; SameSite=Strict`;
}

/** The stored session end, if any and valid. */
export function readSessionExpiry(): string | null {
  const value = readCookie(SESSION_EXPIRES_COOKIE);
  return value && Number.isFinite(Date.parse(value)) ? value : null;
}

export function hasSignedInHint(): boolean {
  return readCookie(SIGNED_IN_HINT_COOKIE) === "1";
}
