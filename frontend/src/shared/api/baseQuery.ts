import {
  fetchBaseQuery,
  type BaseQueryFn,
  type FetchArgs,
} from "@reduxjs/toolkit/query/react";
import { sessionEnded } from "@/features/auth/authSlice";
import { toApiError, type ApiError } from "./apiError";
import { clearSignedInHint, CSRF_COOKIE, CSRF_HEADER, readCookie } from "./cookies";

export const API_BASE = "/api/v1";
const CSRF_INVALID = "/problems/csrf-token-invalid";
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS"]);

/**
 * Absolute same-origin API URL. The browser would resolve the relative path the same way;
 * resolving it explicitly also works where `Request` refuses relative URLs (Node, tests).
 */
function apiUrl(path: string): string {
  const origin = typeof window === "undefined" ? "" : window.location.origin;
  return `${origin}${API_BASE}${path}`;
}

const rawBaseQuery = fetchBaseQuery({
  // Same origin as the page; Next.js rewrites /api/* to Spring (no CORS). URLs come from apiUrl().
  baseUrl: "",
  // The browser sends the httpOnly access_token cookie itself. JavaScript never sees it.
  credentials: "same-origin",
});

/** Ask Spring for a CSRF token; it also (re)sets the readable XSRF-TOKEN cookie. */
async function fetchCsrfToken(): Promise<string | undefined> {
  try {
    const response = await fetch(apiUrl("/auth/csrf"), {
      credentials: "same-origin",
      headers: { Accept: "application/json" },
    });
    if (!response.ok) return readCookie(CSRF_COOKIE);
    const body = (await response.json()) as { token?: string };
    return readCookie(CSRF_COOKIE) ?? body.token;
  } catch {
    return undefined;
  }
}

/**
 * The token for the NEXT write. Read FRESH from the cookie every time: Spring issues a new
 * one after every write, login and logout, so a cached value would go stale.
 */
async function currentCsrfToken(): Promise<string | undefined> {
  return readCookie(CSRF_COOKIE) ?? (await fetchCsrfToken());
}

function withHeader(args: FetchArgs, name: string, value: string): FetchArgs {
  const headers = new Headers(args.headers as HeadersInit | undefined);
  headers.set(name, value);
  return { ...args, headers };
}

/**
 * baseQuery for every endpoint (architecture.md §3.4, security.md §4.2.1):
 * - non-GET requests carry `X-XSRF-TOKEN` (fetching a token first if there's no cookie);
 * - a 403 `csrf-token-invalid` gets a fresh token and ONE retry;
 * - errors become a serializable `ApiError` (ProblemDetail + Retry-After);
 * - a 401 ends the client session (Redux auth state + signed-in hint).
 */
export const baseQuery: BaseQueryFn<string | FetchArgs, unknown, ApiError> = async (
  args,
  api,
  extraOptions,
) => {
  const given: FetchArgs = typeof args === "string" ? { url: args } : args;
  const request: FetchArgs = { ...given, url: apiUrl(given.url) };
  const method = (request.method ?? "GET").toUpperCase();
  const needsCsrf = !SAFE_METHODS.has(method);

  const send = async (forceNewToken: boolean) => {
    if (!needsCsrf) return rawBaseQuery(request, api, extraOptions);
    const token = forceNewToken ? await fetchCsrfToken() : await currentCsrfToken();
    return rawBaseQuery(token ? withHeader(request, CSRF_HEADER, token) : request, api, extraOptions);
  };

  let result = await send(false);
  if (
    needsCsrf &&
    result.error?.status === 403 &&
    (result.error.data as { type?: string } | undefined)?.type === CSRF_INVALID
  ) {
    result = await send(true);
  }

  if (result.error) {
    const error = toApiError(result.error, result.meta);
    if (error.status === 401) {
      clearSignedInHint();
      api.dispatch(sessionEnded());
    }
    return { error, meta: result.meta };
  }
  return { data: result.data, meta: result.meta };
};
