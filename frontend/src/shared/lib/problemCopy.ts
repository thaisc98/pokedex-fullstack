import type { components } from "@/shared/api/schema";

export type ProblemDetail = components["schemas"]["ProblemDetail"];

export type ProblemCopy = { title: string; message: string };

/**
 * ProblemDetail.type → user-facing copy (docs/ui-ux.md §8). One place for all
 * error wording. Types come from docs/api/openapi.yaml (`ProblemDetail.type`).
 */
const COPY: Readonly<Record<string, ProblemCopy>> = {
  "/problems/validation-error": {
    title: "Please check your input",
    message: "Some of the values aren’t valid.",
  },
  "/problems/unauthorized": {
    title: "Please sign in",
    message: "You need to sign in to do that.",
  },
  "/problems/invalid-credentials": {
    title: "Couldn’t sign you in",
    message: "Email or password is incorrect.",
  },
  "/problems/forbidden": {
    title: "Not allowed",
    message: "Your account can’t do that.",
  },
  "/problems/csrf-token-invalid": {
    title: "Your session needs a refresh",
    message: "Please reload the page and try again.",
  },
  "/problems/not-found": {
    title: "We couldn’t find that Pokémon",
    message: "Check the name or number and try again, or browse the catalog.",
  },
  "/problems/email-taken": {
    title: "Email already registered",
    message: "An account with this email already exists. Try signing in.",
  },
  "/problems/rate-limited": {
    title: "Too many attempts",
    message: "Please wait a moment and try again.",
  },
  "/problems/version-conflict": {
    title: "Someone else updated this record",
    message: "Reload to see the latest version.",
  },
  "/problems/payload-too-large": {
    title: "That’s too much data",
    message: "Please shorten your input and try again.",
  },
  "/problems/unsupported-media-type": {
    title: "Request not accepted",
    message: "The request was sent in a format the server doesn’t accept.",
  },
  "/problems/upstream-unavailable": {
    title: "PokeAPI is unavailable",
    message: "We couldn’t reach the Pokémon data source. Please try again in a moment.",
  },
  // Frontend-only: the Next.js server couldn't reach the Spring API at all.
  "urn:pokedex:frontend:backend-unreachable": {
    title: "The Pokédex is temporarily unavailable",
    message: "We couldn’t reach our server. Please try again in a moment.",
  },
  "/problems/internal-error": {
    title: "Something went wrong on our side",
    message: "Please try again. If it keeps happening, share the reference below.",
  },
};

const BY_STATUS: Readonly<Record<number, string>> = {
  400: "/problems/validation-error",
  401: "/problems/unauthorized",
  403: "/problems/forbidden",
  404: "/problems/not-found",
  429: "/problems/rate-limited",
  503: "/problems/upstream-unavailable",
};

const GENERIC: ProblemCopy = {
  title: "Something went wrong",
  message: "Please try again.",
};

/** Friendly copy for a problem. Unknown types fall back to `detail`, then to the status, then generic. */
export function problemCopy(
  problem: Pick<ProblemDetail, "type" | "status" | "detail"> | undefined,
): ProblemCopy {
  if (!problem) return GENERIC;
  const known = problem.type ? COPY[problem.type] : undefined;
  if (known) return known;
  if (problem.detail) return { title: GENERIC.title, message: problem.detail };
  const byStatus = BY_STATUS[problem.status];
  return (byStatus && COPY[byStatus]) || GENERIC;
}
