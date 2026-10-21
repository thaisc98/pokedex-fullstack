import { NextResponse, type NextRequest } from "next/server";

/** Same name as `SIGNED_IN_HINT_COOKIE` in shared/api/cookies.ts (kept local: proxy stays dependency-free). */
const SIGNED_IN_HINT_COOKIE = "pokedex_signed_in";

/**
 * Per-request CSP nonce + security headers (docs/security.md §4.7, §5; Next.js CSP guide).
 *
 * This file only does optimistic, UX-level work. It never makes authorization
 * decisions: the Spring API enforces every rule (security.md §3.3).
 */

function createNonce(): string {
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  return btoa(String.fromCharCode(...bytes));
}

function isHttps(request: NextRequest): boolean {
  // Behind a TLS-terminating proxy the original scheme arrives in X-Forwarded-Proto.
  const forwarded = request.headers.get("x-forwarded-proto")?.split(",")[0]?.trim();
  return (forwarded ?? request.nextUrl.protocol.replace(":", "")) === "https";
}

function buildCsp(nonce: string, { isDev, https }: { isDev: boolean; https: boolean }) {
  return [
    "default-src 'self'",
    // 'unsafe-eval' only in development: React uses eval for debugging (Next.js CSP guide).
    `script-src 'self' 'nonce-${nonce}' 'strict-dynamic'${isDev ? " 'unsafe-eval'" : ""}`,
    `style-src 'self' 'nonce-${nonce}'`,
    "img-src 'self' data:",
    "connect-src 'self'",
    "font-src 'self'",
    "frame-ancestors 'none'",
    "base-uri 'self'",
    "form-action 'self'",
    "object-src 'none'",
    // Only over HTTPS: on plain http://localhost it would upgrade /_next assets and break the page.
    ...(https ? ["upgrade-insecure-requests"] : []),
  ].join("; ");
}

export function proxy(request: NextRequest) {
  const nonce = createNonce();
  const https = isHttps(request);
  const csp = buildCsp(nonce, { isDev: process.env.NODE_ENV === "development", https });

  // Next.js reads the nonce from the request's CSP header and applies it to its scripts.
  const requestHeaders = new Headers(request.headers);
  requestHeaders.set("x-nonce", nonce);
  requestHeaders.set("Content-Security-Policy", csp);

  // Optimistic, UX-only redirect (security.md §3.3): no sign-in hint → go to login first.
  // `access_token` is `Path=/api`, so it is never sent with page requests; we check the
  // `pokedex_signed_in` hint the app sets after login. Never an authorization decision:
  // the Spring API still answers 401/403 on its own.
  const { pathname, search } = request.nextUrl;
  if (
    (pathname === "/collection" || pathname.startsWith("/collection/")) &&
    request.cookies.get(SIGNED_IN_HINT_COOKIE)?.value !== "1"
  ) {
    const login = new URL("/login", request.url);
    login.searchParams.set("from", `${pathname}${search}`);
    return NextResponse.redirect(login);
  }

  const response = NextResponse.next({ request: { headers: requestHeaders } });
  response.headers.set("Content-Security-Policy", csp);
  response.headers.set("X-Content-Type-Options", "nosniff");
  response.headers.set("X-Frame-Options", "DENY");
  response.headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  response.headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
  if (https) {
    response.headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
  }
  return response;
}

export const config = {
  matcher: [
    /*
     * All paths except /api (Spring sets its own headers), static files, the image
     * optimizer and the favicon. Prefetches skip the proxy (Next.js CSP guide).
     */
    {
      source: "/((?!api|_next/static|_next/image|favicon.ico).*)",
      missing: [
        { type: "header", key: "next-router-prefetch" },
        { type: "header", key: "purpose", value: "prefetch" },
      ],
    },
  ],
};
