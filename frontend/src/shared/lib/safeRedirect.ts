/**
 * Open-redirect guard for `?from=` (security.md §5): only a same-origin PATH is accepted
 * (starts with a single `/`, no scheme, no `//` or `/\`, no control characters).
 * Anything else returns the fallback.
 */
export function safeRedirect(from: string | null | undefined, fallback = "/collection"): string {
  if (typeof from !== "string" || from.length === 0 || from.length > 2048) return fallback;
  if (!from.startsWith("/")) return fallback;
  if (from.startsWith("//") || from.startsWith("/\\")) return fallback;
  // Control characters (incl. tab/newline, which browsers strip, turning "/\t/evil" into "//evil").
  if (/[\u0000-\u001f\u007f]/.test(from)) return fallback;
  try {
    const url = new URL(from, "http://internal.invalid");
    if (url.origin !== "http://internal.invalid") return fallback;
    // Never bounce back to the auth pages themselves.
    if (url.pathname === "/login" || url.pathname === "/register") return fallback;
    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return fallback;
  }
}
