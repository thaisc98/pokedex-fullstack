import { render, waitFor } from "@testing-library/react";
import { http, HttpResponse } from "msw";
import { Provider } from "react-redux";
import { afterEach, describe, expect, it, vi } from "vitest";
import { server } from "@/mocks/node";
import { makeStore } from "@/store/makeStore";
import { SessionBootstrap } from "./SessionBootstrap";

const pathname = vi.hoisted(() => ({ current: "/pokemon" }));
vi.mock("next/navigation", () => ({ usePathname: () => pathname.current }));

const API = "http://localhost:3000/api/v1";
const user = { id: "u1", email: "ash@example.com", displayName: "Ash", role: "USER" as const };

function renderWithStore() {
  const store = makeStore();
  render(
    <Provider store={store}>
      <SessionBootstrap />
    </Provider>,
  );
  return store;
}

afterEach(() => {
  document.cookie = "pokedex_signed_in=; path=/; max-age=0";
});

describe("SessionBootstrap", () => {
  it("should_restore_the_user_with_auth_me_when_the_signed_in_hint_is_set", async () => {
    pathname.current = "/pokemon";
    document.cookie = "pokedex_signed_in=1; path=/";
    server.use(http.get(`${API}/auth/me`, () => HttpResponse.json(user)));

    const store = renderWithStore();

    await waitFor(() => expect(store.getState().auth.user).toEqual(user));
  });

  it("should_not_call_auth_me_for_visitors", async () => {
    pathname.current = "/pokemon";
    // No handler: an unexpected request would fail the test (onUnhandledRequest: error).
    const store = renderWithStore();

    await waitFor(() => expect(store.getState().auth.checked).toBe(true));
    expect(store.getState().auth.user).toBeNull();
  });

  it("should_skip_the_probe_and_drop_a_stale_hint_on_the_login_page", async () => {
    pathname.current = "/login";
    document.cookie = "pokedex_signed_in=1; path=/";

    const store = renderWithStore();

    await waitFor(() => expect(store.getState().auth.checked).toBe(true));
    expect(document.cookie).not.toContain("pokedex_signed_in=1");
  });
});
