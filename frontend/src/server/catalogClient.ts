import "server-only";

import { headers } from "next/headers";

import type { PokemonDetail, PokemonPage } from "@/features/catalog/types";
import type { CatalogParams } from "@/features/catalog/lib/catalogParams";
import type { ProblemDetail } from "@/shared/lib/problemCopy";

/**
 * Server-to-server client for the PUBLIC catalog (architecture.md §3.1–3.2).
 * Never reads or forwards the user's cookies.
 */

/** Problem type used when the Spring API itself can't be reached (not part of the contract). */
export const BACKEND_UNREACHABLE = "urn:pokedex:frontend:backend-unreachable";

export class CatalogError extends Error {
  constructor(readonly problem: ProblemDetail) {
    super(problem.title);
    this.name = "CatalogError";
  }
  get status() {
    return this.problem.status;
  }
}

// Read at RUNTIME on every call (not at build time), so one image works in any environment.
function baseUrl(): string {
  const env = process.env;
  return (env["BACKEND_INTERNAL_URL"] ?? "http://localhost:8080").replace(/\/+$/, "");
}

const ID_OR_NAME = /^[A-Za-z0-9-]{1,50}$/;

/**
 * Headers for the server-to-server call. We pass along the X-Forwarded-For value of the
 * incoming request (Next.js fills it with the socket address when no proxy in front set it),
 * so the backend can rate-limit per real client IP. We never add or invent addresses here.
 * Never forwarded: cookies or Authorization (the catalog is public; architecture.md §3.1).
 */
async function upstreamHeaders(): Promise<HeadersInit> {
  const result: Record<string, string> = { Accept: "application/json, application/problem+json" };
  try {
    const forwardedFor = (await headers()).get("x-forwarded-for");
    if (forwardedFor) result["X-Forwarded-For"] = forwardedFor;
  } catch {
    // Outside a request scope (e.g. a build-time call): nothing to forward.
  }
  return result;
}

async function getJson<T>(path: string): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl()}/api/v1${path}`, {
      headers: await upstreamHeaders(),
      // No Next.js data cache: the headers differ per client, and the API's Caffeine cache
      // already makes repeat calls cheap.
      cache: "no-store",
      signal: AbortSignal.timeout(15_000),
    });
  } catch {
    throw new CatalogError({
      type: BACKEND_UNREACHABLE,
      title: "Service unavailable",
      status: 503,
    });
  }
  if (response.ok) return (await response.json()) as T;

  let problem: ProblemDetail | undefined;
  try {
    problem = (await response.json()) as ProblemDetail;
  } catch {
    problem = undefined;
  }
  throw new CatalogError({
    type: problem?.type ?? "about:blank",
    title: problem?.title ?? response.statusText,
    status: response.status,
    detail: problem?.detail,
    correlationId:
      typeof problem?.correlationId === "string"
        ? problem.correlationId
        : (response.headers.get("x-request-id") ?? undefined),
  });
}

/** One catalog page. `page` is 1-based (URL); the API is 0-based. */
export function getCatalogPage({ page, size }: CatalogParams): Promise<PokemonPage> {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) });
  return getJson<PokemonPage>(`/pokemon?${query}`);
}

export function getPokemonDetail(idOrName: string): Promise<PokemonDetail> {
  if (!ID_OR_NAME.test(idOrName)) {
    // Same answer the API would give (400), without a network round trip.
    return Promise.reject(
      new CatalogError({ type: "/problems/validation-error", title: "Bad request", status: 400 }),
    );
  }
  return getJson<PokemonDetail>(`/pokemon/${encodeURIComponent(idOrName.toLowerCase())}`);
}
