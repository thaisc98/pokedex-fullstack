import type { SearchResult } from "../types";

const QUERY = /^[a-z0-9-]{1,50}$/;

/**
 * The search query the API accepts: trimmed, lowercased, spaces → hyphens, 1–50 chars of
 * `[a-z0-9-]`. Returns null for anything else (no request is sent).
 */
export function normalizeSearch(raw: string): string | null {
  const q = raw.trim().toLowerCase().replace(/\s+/g, "-");
  return QUERY.test(q) ? q : null;
}

/**
 * Where Enter should go for the typed text: a Pokédex number goes straight to its page; a
 * name only when it exactly matches one of the loaded results. Otherwise null.
 */
export function exactMatchHref(raw: string, results: readonly SearchResult[]): string | null {
  const q = normalizeSearch(raw);
  if (!q) return null;
  if (/^\d+$/.test(q)) {
    const id = Number(q);
    return id > 0 ? `/pokemon/${id}` : null;
  }
  const hit = results.find((r) => r.name === q);
  return hit ? `/pokemon/${hit.id}` : null;
}
