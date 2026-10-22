import { render, type RenderOptions } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactElement } from "react";
import { Provider } from "react-redux";
import { sessionEnded, signedIn } from "@/features/auth/authSlice";
import type { UserDto } from "@/shared/api/types";
import { makeStore } from "@/store/makeStore";

export const API = "http://localhost:3000/api/v1";

export const trainer: UserDto = { id: "u-1", email: "ash@example.com", displayName: "Ash", role: "USER" };
export const admin: UserDto = { id: "u-2", email: "oak@example.com", displayName: "Prof. Oak", role: "ADMIN" };

/**
 * Renders with a fresh store (frontend/AGENTS.md: fresh makeStore() per test).
 * `signedInAs` puts a user in the session; `null` means a known visitor.
 */
export function renderWithStore(
  ui: ReactElement,
  { signedInAs, ...options }: { signedInAs?: UserDto | null } & Omit<RenderOptions, "wrapper"> = {},
) {
  const store = makeStore();
  if (signedInAs) store.dispatch(signedIn({ user: signedInAs, expiresAt: "2099-01-01T00:00:00Z" }));
  else if (signedInAs === null) store.dispatch(sessionEnded());
  const user = userEvent.setup();
  const result = render(<Provider store={store}>{ui}</Provider>, options);
  return { store, user, ...result };
}

/** Writes a CSRF cookie so writes don't need a GET /auth/csrf handler. */
export function withCsrfCookie() {
  document.cookie = "XSRF-TOKEN=test-token; path=/";
}
