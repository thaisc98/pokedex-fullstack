import type { FetchBaseQueryError, FetchBaseQueryMeta } from "@reduxjs/toolkit/query";
import type { ProblemDetail } from "@/shared/lib/problemCopy";

/** Problem type for "the browser couldn't reach the API at all" (not part of the contract). */
export const NETWORK_ERROR_TYPE = "urn:pokedex:frontend:backend-unreachable";

/**
 * The error every endpoint rejects with: a plain, serializable object (RTK's dev checks
 * stay quiet). `retryAfter` comes from the `Retry-After` header on 429s.
 */
export type ApiError = {
  status: number;
  problem: ProblemDetail;
  retryAfter?: number;
};

function isProblem(value: unknown): value is ProblemDetail {
  return typeof value === "object" && value !== null && "status" in value && "title" in value;
}

export function toApiError(error: FetchBaseQueryError, meta?: FetchBaseQueryMeta): ApiError {
  if (typeof error.status === "number") {
    const retryHeader = meta?.response?.headers.get("Retry-After");
    const retryAfter = retryHeader && /^\d+$/.test(retryHeader) ? Number(retryHeader) : undefined;
    const problem: ProblemDetail = isProblem(error.data)
      ? error.data
      : { type: "about:blank", title: "Request failed", status: error.status };
    return { status: error.status, problem, ...(retryAfter !== undefined ? { retryAfter } : {}) };
  }
  // FETCH_ERROR, TIMEOUT_ERROR, PARSING_ERROR, CUSTOM_ERROR
  return {
    status: 0,
    problem: { type: NETWORK_ERROR_TYPE, title: "Network error", status: 503 },
  };
}

export function isApiError(value: unknown): value is ApiError {
  return (
    typeof value === "object" &&
    value !== null &&
    "status" in value &&
    "problem" in value &&
    typeof (value as ApiError).status === "number"
  );
}

/** Field violations from a 400, keyed by the API's field path (e.g. `customAttributes.tags[2]`). */
export function fieldErrors(error: ApiError): { field: string; message: string }[] {
  const raw = (error.problem as { errors?: unknown }).errors;
  if (!Array.isArray(raw)) return [];
  return raw.filter(
    (e): e is { field: string; message: string } =>
      typeof e === "object" && e !== null && typeof e.field === "string" && typeof e.message === "string",
  );
}
