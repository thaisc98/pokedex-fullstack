import { createHash } from "node:crypto";
import { tmpdir } from "node:os";
import path from "node:path";

/**
 * Accounts and paths for the E2E suite. Credentials come from the environment; the defaults are
 * the placeholder DEMO_* values from .env.example (never real secrets). The demo seed creates them.
 */
export const accounts = {
  user: {
    email: process.env.E2E_USER_EMAIL ?? "trainer@pokedex.local",
    password: process.env.E2E_USER_PASSWORD ?? "change-me-demo-user",
  },
  admin: {
    email: process.env.E2E_ADMIN_EMAIL ?? "admin@pokedex.local",
    password: process.env.E2E_ADMIN_PASSWORD ?? "change-me-demo-admin",
  },
} as const;

/**
 * Accounts the US03 journey logs in with through the UI: one per viewport project, so each email
 * sees one login per run (security.md §4.1 allows 5 per minute per IP + email). Registered once by
 * the setup project. The password is a test-only value for throwaway accounts on a local stack.
 */
export const US03_PASSWORD = "E2E-Journey-Password-2026";
export function us03Email(project: string) {
  return `e2e-us03-${project}@example.com`;
}
export const VIEWPORT_PROJECTS = ["mobile-375", "tablet-768", "desktop-1280"] as const;

const baseURL = process.env.E2E_BASE_URL ?? "http://localhost:3000";

/** Saved sessions live outside the repo, keyed by the stack under test. */
export const AUTH_DIR = path.join(
  tmpdir(),
  "pokedex-e2e-auth",
  createHash("sha256").update(baseURL).digest("hex").slice(0, 12),
);
export const USER_STATE = path.join(AUTH_DIR, "user.json");
export const ADMIN_STATE = path.join(AUTH_DIR, "admin.json");
export const US03_MARKER = path.join(AUTH_DIR, "us03-accounts.json");
